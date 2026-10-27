# SMS-IA — Android starter V1

Application Android native Kotlin, français, conçue comme une première version testable.

## Fonctionnalités
- Mode mixte / suggestions uniquement / automatisation limitée (réglage d'interface).
- Sélection de réseaux souhaités : WhatsApp, Messenger, Facebook, Snapchat.
- Champ contact de test (Ami 1) et réglages humour/drague.
- Génération de trois suggestions par l'API Gemini.
- Réglages sauvegardés localement.
- Accès optionnel aux paramètres d'accès aux notifications Android.
- Workflow GitHub Actions pour construire `app-debug.apk`.

## Limites importantes de cette V1
- Les cases de réseaux ne connectent pas les comptes.
- Le contact « Ami 1 » est un exemple fictif, pas un contact importé du téléphone.
- Le service de notification ne lit pas les historiques et n'envoie aucun message automatiquement.
- Les boutons WhatsApp/Messenger ne font qu'ouvrir les applications si elles sont installées.
- Facebook/Messenger/Snapchat n'offrent pas une API générale autorisant une application tierce à lire et répondre à tous les messages privés. Une automatisation fiable dépend d'intégrations officielles et/ou d'actions de réponse offertes par chaque notification.
- La clé Gemini est stockée dans les préférences privées de l'application dans ce prototype. Ne redistribue pas l'APK avec une clé intégrée. Pour une publication, déplacer les appels Gemini dans un backend sécurisé avec authentification et limites de coût.
- L'envoi automatique n'est pas activé dans cette V1 : les suggestions doivent être vérifiées et envoyées par l'utilisateur.

## Générer l'APK
1. Crée un dépôt GitHub et téléverse le contenu du dossier.
2. Ouvre l'onglet **Actions**.
3. Lance **Build SMS-IA APK** via **Run workflow** ou pousse un commit sur `main`.
4. Télécharge l'artifact **SMS-IA-debug-APK** lorsque le workflow réussit.

## Gemini
Crée une clé dans Google AI Studio, puis colle-la dans les réglages de l'application. Le modèle utilisé dans le starter est `gemini-2.5-flash`. La disponibilité dépend du compte, de la région et des quotas Google.
