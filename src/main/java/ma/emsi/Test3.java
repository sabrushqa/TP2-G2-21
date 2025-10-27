package ma.emsi;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.CosineSimilarity;

import java.time.Duration;

public class Test3 {

    private static final String MODEL_NAME = "gemini-embedding-001";

    // CORRECTION : Lit la valeur de la variable d'environnement nommée "GEMINI"
    private static final String API_KEY = System.getenv("GEMINI");

    public static void main(String[] args) {

        // Ajout d'une vérification pour s'assurer que la variable d'env est lue
        if (API_KEY == null || API_KEY.isEmpty()) {
            System.err.println("ERREUR : La variable d'environnement 'GEMINI' n'est pas définie ou est vide.");
            System.err.println("Veuillez la définir (ex: export GEMINI=AIzaSy... ) et relancer le programme.");
            return;
        }

        System.out.println("--- Début du Test 3 : Embeddings et Similarité Cosinus ---");

        // 1. Configuration du Modèle d'Embeddings (Builder Pattern)
        EmbeddingModel embeddingModel = GoogleAiEmbeddingModel.builder()
                // La clé est maintenant la VALEUR lue de la variable d'environnement
                .apiKey(API_KEY)
                .modelName(MODEL_NAME)
                .timeout(Duration.ofSeconds(20))
                .build();

        System.out.println("Modèle configuré : " + MODEL_NAME + " (Clé lue via la variable d'environnement 'GEMINI')");

        // Définition des phrases
        String similarPhraseA = "Le développeur écrit du code pour le logiciel.";
        String similarPhraseB = "L'ingénieur programme l'application informatique.";
        String differentPhraseX = "Le pain est cuit dans un four traditionnel.";
        String differentPhraseY = "La fusée s'est envolée vers la lune.";

        // Calcul des similarités
        calculateAndDisplaySimilarity(embeddingModel, similarPhraseA, similarPhraseB, "SIMILAIRES");
        calculateAndDisplaySimilarity(embeddingModel, differentPhraseX, differentPhraseY, "DIFFÉRENTES");
    }

    private static void calculateAndDisplaySimilarity(
            EmbeddingModel embeddingModel,
            String phrase1,
            String phrase2,
            String type) {

        System.out.println("\n--- Calcul de Similarité : " + type + " ---");
        System.out.println("Phrase 1 : \"" + phrase1 + "\"");
        System.out.println("Phrase 2 : \"" + phrase2 + "\"");

        try {
            Response<Embedding> response1 = embeddingModel.embed(phrase1);
            Embedding embedding1 = response1.content();

            Response<Embedding> response2 = embeddingModel.embed(phrase2);
            Embedding embedding2 = response2.content();

            double similarity = CosineSimilarity.between(embedding1, embedding2);

            System.out.println("Dimension des vecteurs : " + embedding1.vector().length);
            System.out.printf("Similarité Cosinus : %.6f\n", similarity);

        } catch (Exception e) {
            // L'erreur sera maintenant affichée seulement si la variable d'env est mal définie ou la clé est invalide
            System.err.println("\nERREUR lors de l'appel API. Vérifiez que la variable d'env 'GEMINI' contient une clé valide.");
        }
    }
}