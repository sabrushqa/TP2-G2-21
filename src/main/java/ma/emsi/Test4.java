package ma.emsi;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters; // New import for chunking
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel; // New import
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiEmbeddingModel; // New import
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.time.Duration;

/**
 * Le RAG facile !
 */
public class Test4 {

    // Assistant conversationnel
    interface Assistant {
        // Prend un message de l'utilisateur et retourne une réponse du LLM.
        String chat(String userMessage);
    }

    public static void main(String[] args) {
        // 1. Récupération de la clé API
        // L'API Key doit être définie dans une variable d'environnement nommée 'GEMINI'
        String apiKey = System.getenv("GEMINI");

        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.err.println("ERREUR FATALE: La clé API 'GEMINI' n'est pas définie dans les variables d'environnement.");
            // Arrêter l'application si la clé est manquante.
            return;
        }

        // 2. Initialisation du ChatModel (pour la conversation)
        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .modelName("gemini-2.5-flash")
                .temperature(0.3) // ≤ 0.3 pour des réponses plus précises
                .timeout(Duration.ofSeconds(5))
                .apiKey(apiKey)
                .build();

        // 3. Initialisation de l'EmbeddingModel (pour le RAG)
        // Ce modèle est nécessaire pour convertir le texte du document et de la question en vecteurs.
        EmbeddingModel embeddingModel = GoogleAiGeminiEmbeddingModel.builder()
                // Modèle d'embedding recommandé par Google AI
                .modelName("text-embedding-004")
                .apiKey(apiKey)
                .build();

        // 4. Ingestion des Données (RAG Setup)

        // Chargement du document
        String nomDocument = "infos.txt";
        Document document = FileSystemDocumentLoader.loadDocument(nomDocument);

        // Création du magasin de vecteurs en mémoire
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // Création de l'Ingestor (qui utilise l'EmbeddingModel)
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(200, 50)) // Divise le document en petits segments (chunks)
                .embeddingModel(embeddingModel)                         // Le modèle pour créer les embeddings (Vecteurs)
                .embeddingStore(embeddingStore)                         // Le magasin pour stocker les embeddings
                .build();

        // Lancement de l'ingestion (calcul des embeddings et stockage)
        ingestor.ingest(document);

        // 5. Création du Retriever
        // Le retriever est l'outil qui va chercher les informations pertinentes dans l'embeddingStore.
        EmbeddingStoreContentRetriever contentRetriever =
                EmbeddingStoreContentRetriever.builder()
                        .embeddingStore(embeddingStore)
                        .embeddingModel(embeddingModel) // Doit utiliser le même modèle d'embedding
                        .maxResults(2) // Récupère les 2 fragments de document les plus pertinents
                        .build();

        // 6. Création de l'Assistant conversationnel
        Assistant assistant =
                AiServices.builder(Assistant.class)
                        .chatModel(chatModel)
                        .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                        .contentRetriever(contentRetriever) // Active la RAG pour l'assistant
                        .build();

        // 7. Exécution de la requête
        String question = "Comment s'appelle le chat de Pierre ?";
        System.out.println("Question: " + question);

        // L'assistant utilise le RAG pour retrouver l'info et le LLM pour formuler la réponse.
        String reponse = assistant.chat(question);

        // Affiche la réponse du LLM.
        System.out.println("Réponse: " + reponse);
    }
}