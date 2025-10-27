package ma.emsi;

import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;

public class Test6 {

    public static void main(String[] args) {
        // --- Configuration du Logging ---
        // Les logs détaillés des requêtes et réponses LLM seront affichés pour l'analyse.
        // Assurez-vous d'avoir configuré logback/slf4j dans votre projet pour voir les logs.

        // 1️⃣ Création directe du modèle Gemini
        GoogleAiGeminiChatModel model = GoogleAiGeminiChatModel.builder()
                // ATTENTION: Utiliser la variable d'environnement standard GEMINI_KEY
                .apiKey(System.getenv("GEMINI"))
                .modelName("gemini-2.0-flash")
                // Activation du logging pour analyser l'utilisation de l'outil
                .logRequestsAndResponses(true)
                .build();

        // 2️⃣ Création de l’assistant avec l’outil météo
        // L'appel à .tools(new MeteoTool()) rend la méthode meteo() accessible au LLM.
        AssistantMeteo assistant = AiServices.builder(AssistantMeteo.class)
                .chatModel(model)
                .tools(new MeteoTool())  // Ajout de l'outil MeteoTool
                .build();

        // 3️⃣ Exécution des tests

        // Test 1 : Météo d'une vraie ville (devrait utiliser l'outil)
        System.out.println("---- Test 1 : Météo d'une vraie ville (Paris) ----");
        System.out.println("Question: Quel temps fait-il à Paris ?");
        System.out.println("Réponse: " + assistant.repondre("Quel temps fait-il à Paris ?"));

        // Test 1 bis : Question indirecte (devrait utiliser l'outil)
        System.out.println("\n---- Test 1 bis : Question indirecte (Rabat) ----");
        System.out.println("Question: J'ai prévu d'aller aujourd'hui à Rabat. Est-ce que tu me conseilles de prendre un parapluie ?");
        System.out.println("Réponse: " + assistant.repondre("J'ai prévu d'aller aujourd'hui à Rabat. Est-ce que tu me conseilles de prendre un parapluie ?"));

        // Test 2 : Ville inexistante (devrait utiliser l'outil, recevoir une erreur/une réponse générique, et répondre en conséquence)
        System.out.println("\n---- Test 2 : Ville inexistante (MarsCity) ----");
        System.out.println("Question: Quel temps fait-il à MarsCity?");
        System.out.println("Réponse: " + assistant.repondre("Quel temps fait-il à MarsCity?"));

        // Test 3 : Requête sans rapport (ne devrait PAS utiliser l'outil)
        System.out.println("\n---- Test 3 : Question sans rapport ----");
        System.out.println("Question: Raconte-moi une blague sur les nuages !");
        System.out.println("Réponse: " + assistant.repondre("Raconte-moi une blague sur les nuages !"));
    }
}
