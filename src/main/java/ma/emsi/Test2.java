package ma.emsi;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import java.util.HashMap;
import java.util.Map;

public class Test2 {

    public static void main(String[] args) {

        String apikey = System.getenv("GEMINI");
        ChatModel chatModel= GoogleAiGeminiChatModel.builder()
                .apiKey(apikey)
                .modelName("gemini-2.5-flash")
                .build();
        String templateString = "Traduis le texte suivant en anglais : \"{{texte_a_traduire}}\"";
        PromptTemplate translatorTemplate = PromptTemplate.from(templateString);

        String texteATraduire = "Bonjour je m'appelle sabrine.";

        Map<String, Object> variables = new HashMap<>();
        variables.put("texte_a_traduire", texteATraduire);

        Prompt finalPrompt = translatorTemplate.apply(variables);

        String finalPromptText = finalPrompt.text();

        System.out.println("Template utilisé : " + translatorTemplate.template());
        System.out.println("---");
        System.out.println("Texte à traduire : " + texteATraduire);
        System.out.println("---");

        // CORRECTION 1: Pass the final prompt text directly to chatModel.chat()
        String response = chatModel.chat(finalPromptText);

        System.out.println("Prompt final (texte envoyé au LLM) : ");
        System.out.println(finalPromptText);

        // CORRECTION 2: Print the actual response/translation from the LLM
        System.out.println("---");
        System.out.println("Réponse du LLM (Traduction) : ");
        System.out.println(response);
    }
}