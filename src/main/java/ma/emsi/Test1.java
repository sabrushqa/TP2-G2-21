package ma.emsi;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

public class Test1 {


    public static void main(String[] args) {

        String apiKey = System.getenv("GEMINI");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("ERREUR : La variable d'environnement GEMINI n'est pas définie.");
            System.err.println("Veuillez la définir avec votre clé API Google AI.");
            return;
        }


        ChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-flash")
                .temperature(0.7)
                .build();



        String question = "Comment s'appelle le chat de Pierre ? ";

        System.out.println(" Question posée au modèle : " + question);



        try {

            String reponse = model.chat(question);

            System.out.println(
                    " Réponse du modèle :\n" + reponse);

        } catch (Exception e) {
            System.err.println("Une erreur s'est produite lors de l'appel à l'API Gemini : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
