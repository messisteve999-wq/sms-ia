package com.steve.smsia

import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private val bg = Color.rgb(8, 17, 24)
    private val panel = Color.rgb(24, 39, 49)
    private val mint = Color.rgb(0, 220, 165)
    private lateinit var apiKey: EditText
    private lateinit var incoming: EditText
    private lateinit var contact: EditText
    private lateinit var result: TextView
    private lateinit var mode: Spinner
    private lateinit var humor: SeekBar
    private lateinit var flirt: SeekBar
    private lateinit var status: TextView
    private val networks = listOf("WhatsApp", "Messenger", "Facebook", "Snapchat")
    private val networkChecks = mutableMapOf<String, CheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("smsia", MODE_PRIVATE)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(22, 20, 22, 28); setBackgroundColor(bg) }
        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)

        root.addView(TextView(this).apply { text = "✦  SMS-IA"; textSize = 30f; setTextColor(mint); typeface = Typeface.DEFAULT_BOLD })
        root.addView(TextView(this).apply { text = "Ton assistant Gemini • drôle, complice et charmeur"; textSize = 14f; setTextColor(Color.LTGRAY); setPadding(0, 4, 0, 18) })
        status = TextView(this).apply { text = "Mode mixte • Suggestions activées"; setTextColor(mint); textSize = 14f; setPadding(14, 12, 14, 12); setBackgroundColor(panel) }
        root.addView(status, matchWrap())

        section(root, "1. Mode de réponse")
        mode = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("Mode mixte (recommandé)", "Suggestions uniquement", "Automatisation limitée aux notifications compatibles"))
            setSelection(prefs.getInt("mode", 0))
            setBackgroundColor(Color.WHITE)
        }
        root.addView(mode, matchWrap())
        mode.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                prefs.edit().putInt("mode", position).apply()
                status.text = when(position) { 1 -> "Suggestions uniquement • aucun envoi automatique"; 2 -> "Automatisation limitée • selon notification et contact"; else -> "Mode mixte • Suggestions activées" }
            }
        }

        section(root, "2. Réseaux souhaités")
        root.addView(TextView(this).apply { text = "Coche les réseaux que tu souhaites configurer. Cela ne connecte pas encore tes comptes."; setTextColor(Color.LTGRAY); textSize = 12f })
        networks.forEach { name ->
            val cb = CheckBox(this).apply { text = name; setTextColor(Color.WHITE); buttonTintList = android.content.res.ColorStateList.valueOf(mint); isChecked = prefs.getBoolean("network_$name", name == "WhatsApp") }
            networkChecks[name] = cb
            root.addView(cb)
        }

        section(root, "3. Contacts autorisés")
        contact = EditText(this).apply { hint = "Exemple maquette : Ami 1"; setText(prefs.getString("contact", "Ami 1")); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); setSingleLine(true); setBackgroundColor(panel); setPadding(12, 10, 12, 10) }
        root.addView(contact, matchWrap())
        root.addView(TextView(this).apply { text = "V1 : liste de test manuelle. Les contacts réels ne sont pas importés automatiquement."; setTextColor(Color.LTGRAY); textSize = 12f; setPadding(0, 5, 0, 0) })

        section(root, "4. Personnalité")
        humor = addSlider(root, "Humour", 50, "Sérieux", "Très drôle")
        flirt = addSlider(root, "Drague amicale", 95, "Amical", "Très charmeur")
        humor.progress = prefs.getInt("humor", 50)
        flirt.progress = prefs.getInt("flirt", 95)

        section(root, "5. Clé API Gemini")
        apiKey = EditText(this).apply { hint = "Colle ta clé API Gemini"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD; setText(prefs.getString("api_key", "")); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); setBackgroundColor(panel); setPadding(12, 10, 12, 10) }
        root.addView(apiKey, matchWrap())
        root.addView(TextView(this).apply { text = "Prototype : la clé est stockée localement sur ce téléphone et envoyée à Google pour générer les réponses. Pour une diffusion publique, utiliser un serveur sécurisé."; setTextColor(Color.LTGRAY); textSize = 12f; setPadding(0, 6, 0, 0) })
        button(root, "Enregistrer les réglages") {
            prefs.edit().putString("api_key", apiKey.text.toString().trim()).putString("contact", contact.text.toString()).putInt("humor", humor.progress).putInt("flirt", flirt.progress).putInt("mode", mode.selectedItemPosition).apply()
            networkChecks.forEach { (n, cb) -> prefs.edit().putBoolean("network_$n", cb.isChecked).apply() }
            toast("Réglages enregistrés sur ce téléphone")
        }

        section(root, "6. Fenêtre flottante")
        button(root, "Activer / afficher la bulle SMS-IA") {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            } else {
                startService(Intent(this, FloatingAssistantService::class.java))
                toast("Bulle SMS-IA activée")
            }
        }
        button(root, "Masquer la bulle SMS-IA") {
            stopService(Intent(this, FloatingAssistantService::class.java))
            toast("Bulle SMS-IA arrêtée")
        }

        section(root, "7. Suggestions Gemini")
        incoming = EditText(this).apply { hint = "Colle ici le message reçu…"; minLines = 3; gravity = Gravity.TOP; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); setBackgroundColor(panel); setPadding(12, 12, 12, 12) }
        root.addView(incoming, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
        incoming.setText(intent.getStringExtra("floating_message") ?: "")

        button(root, "Générer 3 réponses") { generateReplies() }
        result = TextView(this).apply { text = "Les réponses apparaîtront ici."; textSize = 15f; setTextColor(Color.WHITE); setPadding(14, 14, 14, 14); setBackgroundColor(panel) }
        root.addView(result, matchWrap())
        button(root, "Activer l'accès aux notifications") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        button(root, "Tester WhatsApp") { openApp("com.whatsapp") }
        button(root, "Tester Messenger") { openApp("com.facebook.orca") }
        if (intent.getBooleanExtra("floating_generate", false)) {
            incoming.post { generateReplies() }
        }
        root.addView(TextView(this).apply { text = "Limites : l'accès aux notifications peut aider à lire certains aperçus. L'envoi direct n'est possible que si l'application source expose une action de réponse compatible. Snapchat et Facebook ne sont pas garantis. Aucun message n'est envoyé silencieusement par ce prototype."; setTextColor(Color.LTGRAY); textSize = 12f; setPadding(0, 16, 0, 0) })
    }

    private fun generateReplies() {
        val key = apiKey.text.toString().trim()
        val msg = incoming.text.toString().trim()
        if (key.isBlank()) { toast("Ajoute d'abord ta clé API Gemini."); return }
        if (msg.isBlank()) { toast("Colle d'abord un message reçu."); return }
        val prefs = getSharedPreferences("smsia", MODE_PRIVATE)
        prefs.edit().putString("api_key", key).putString("contact", contact.text.toString()).putInt("humor", humor.progress).putInt("flirt", flirt.progress).apply()
        val prompt = "Tu es SMS-IA, assistant de rédaction en français. Génère exactement 3 propositions courtes, naturelles et distinctes, numérotées 1 à 3. Humour ${humor.progress}/100; drague amicale ${flirt.progress}/100. La drague reste respectueuse, légère, sans pression ni contenu sexuel explicite. Contexte/contact autorisé de test: ${contact.text}. Message reçu: $msg. Ne prétends pas avoir envoyé le message."
        result.text = "Gemini prépare les suggestions…"
        thread {
            try {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + Uri.encode(key))
                val conn = (url.openConnection() as HttpURLConnection).apply { requestMethod = "POST"; connectTimeout = 20000; readTimeout = 30000; doOutput = true; setRequestProperty("Content-Type", "application/json; charset=UTF-8") }
                val body = JSONObject().put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt))))).toString()
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                val response = stream.bufferedReader().use { it.readText() }
                if (conn.responseCode !in 200..299) throw Exception("Google API (${conn.responseCode}): " + JSONObject(response).optJSONObject("error")?.optString("message", "Erreur API"))
                val text = JSONObject(response).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                runOnUiThread { result.text = text; (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(incoming.windowToken, 0) }
                conn.disconnect()
            } catch (e: Exception) { runOnUiThread { result.text = "Erreur Gemini : ${e.message ?: "connexion impossible"}\nVérifie la clé API, Internet et l'accès au modèle." } }
        }
    }

    private fun addSlider(root: LinearLayout, title: String, initial: Int, low: String, high: String): SeekBar {
        root.addView(TextView(this).apply { text = "$title : $initial %"; setTextColor(Color.WHITE); textSize = 14f })
        val seek = SeekBar(this).apply { max = 100; progress = initial; progressTintList = android.content.res.ColorStateList.valueOf(mint); thumbTintList = android.content.res.ColorStateList.valueOf(mint) }
        root.addView(seek, matchWrap())
        root.addView(TextView(this).apply { text = "$low                                      $high"; setTextColor(Color.LTGRAY); textSize = 11f })
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) { (root.getChildAt(root.indexOfChild(seek)-1) as? TextView)?.text = "$title : $p %" }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        return seek
    }
    private fun section(root: LinearLayout, text: String) { root.addView(TextView(this).apply { this.text = text; textSize = 19f; typeface = Typeface.DEFAULT_BOLD; setTextColor(mint); setPadding(0, 22, 0, 9) }) }
    private fun button(root: LinearLayout, text: String, action: () -> Unit) {
        root.addView(Button(this).apply {
            this.text = text
            isAllCaps = false
            textSize = 15f
            setTextColor(Color.rgb(8, 25, 29))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(mint)
                cornerRadius = 24f
            }
            setPadding(12, 8, 12, 8)
            setOnClickListener { action() }
        }, matchWrap())
    }
    private fun matchWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 8 }
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
    private fun openApp(pkg: String) { val launch = packageManager.getLaunchIntentForPackage(pkg); if (launch != null) startActivity(launch) else toast("Application non installée ou inaccessible.") }
}
