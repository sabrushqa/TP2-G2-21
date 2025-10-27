package ma.emsi;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * TP RAG - Assistant Conversationnel avec Support de Cours Machine Learning
 *
 * FONCTIONNEMENT:
 * 1. Extraction du texte depuis le PDF avec PDFBox 3.x (Loader.loadPDF)
 * 2. Segmentation en morceaux de 800 caracteres avec overlap de 200
 * 3. Conversion de chaque morceau en embedding (vecteur numerique)
 * 4. Stockage dans une base vectorielle en memoire (InMemoryEmbeddingStore)
 * 5. Pour chaque question:
 *    - Recherche des morceaux les plus pertinents (similarite semantique)
 *    - Ajout de ces morceaux au contexte de la question
 *    - Generation de la reponse par le LLM avec ce contexte enrichi
 *
 * PROBLEMES RENCONTRES ET SOLUTIONS:
 *
 * 1. ERREUR: "Cannot resolve method 'load' in 'PDDocument'"
 *    CAUSE: PDFBox 3.x a change l'API
 *    SOLUTION: Utiliser Loader.loadPDF() au lieu de PDDocument.load()
 *
 * 2. ERREUR: "Resource has been exhausted (429)"
 *    CAUSE: Quota API Gemini depasse (trop de requetes)
 *    SOLUTION: Augmenter timeout a 60s, ajouter pauses entre requetes
 *
 * 3. ERREUR: "Le texte fourni est un document PDF corrompu et illisible"
 *    CAUSE: Caracteres speciaux mal encodes dans le PDF
 *    SOLUTION: Nettoyer le texte en supprimant caracteres de controle
 *
 * 4. ERREUR: "Cannot resolve symbol 'ApachePdfBoxDocumentParser'"
 *    CAUSE: Package langchain4j-document-parser-apache-pdfbox n'existe pas en version 1.8.0
 *    SOLUTION: Utiliser PDFBox directement sans passer par LangChain4j
 *
 * @author Votre Nom
 */
public class Test5 {

    interface Assistant {
        @SystemMessage("""
            Tu es un assistant pedagogique specialise en Machine Learning et IA.
           
            """)
        String chat(String userMessage);
    }


    private static String extrairePDF(String cheminFichier) throws Exception {
        File fichierPDF = new File(cheminFichier);

        if (!fichierPDF.exists()) {
            throw new RuntimeException("Le fichier " + cheminFichier + " n'existe pas!");
        }

        System.out.println("Extraction du PDF: " + cheminFichier);

        try (PDDocument document = Loader.loadPDF(fichierPDF)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String texte = stripper.getText(document);

            System.out.println("Pages: " + document.getNumberOfPages());
            System.out.println("Caracteres extraits: " + texte.length());

            if (texte.length() < 100) {
                throw new RuntimeException("PDF vide ou base sur images");
            }

            return texte;
        }
    }

    /**
     * Nettoie le texte en supprimant caracteres problematiques
     */
    private static String nettoyerTexte(String texte) {
        if (texte == null || texte.isEmpty()) return "";

        texte = texte.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", " ");
        texte = texte.replaceAll(" +", " ");
        texte = texte.replaceAll("\\n{3,}", "\n\n");

        return texte.trim();
    }

    public static void main(String[] args) {
        System.out.println("=== TEST 6  - ASSISTANT MACHINE LEARNING ===\n");

        // Verification cle API
        String llmKey = System.getenv("GEMINI");
        if (llmKey == null || llmKey.isEmpty()) {
            System.err.println("ERREUR: Variable GEMINI non definie");

            return;
        }

        try {
            // ETAPE 1: EXTRACTION DU PDF
            System.out.println("[1/5] EXTRACTION DU PDF");
            String nomDocument = "ML.pdf";
            String texteExtrait = extrairePDF(nomDocument);
            texteExtrait = nettoyerTexte(texteExtrait);
            System.out.println("Texte nettoye: " + texteExtrait.length() + " caracteres\n");

            // ETAPE 2: SEGMENTATION
            System.out.println("[2/5] SEGMENTATION");
            Document document = Document.from(texteExtrait);
            DocumentSplitter splitter = DocumentSplitters.recursive(800, 200);
            List<TextSegment> segments = splitter.split(document);
            System.out.println("Segments crees: " + segments.size() + "\n");

            // ETAPE 3: INITIALISATION DES MODELES
            System.out.println("[3/5] INITIALISATION DES MODELES");
            ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                    .modelName("gemini-2.0-flash-exp")
                    .temperature(0.2)
                    .timeout(Duration.ofSeconds(60))
                    .apiKey(llmKey)
                    .build();

            EmbeddingModel embeddingModel = GoogleAiEmbeddingModel.builder()
                    .apiKey(llmKey)
                    .modelName("text-embedding-004")
                    .timeout(Duration.ofSeconds(30))
                    .build();
            System.out.println("Modeles prets\n");

            // ETAPE 4: CREATION BASE VECTORIELLE ET INGESTION
            System.out.println("[4/5] INGESTION DANS BASE VECTORIELLE");
            EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();

            System.out.println("Ingestion de " + segments.size() + " segments...");
            List<Document> documentsToIngest = segments.stream()
                    .map(segment -> Document.from(segment.text(), segment.metadata()))
                    .collect(Collectors.toList());

            long debut = System.currentTimeMillis();
            ingestor.ingest(documentsToIngest);
            long duree = System.currentTimeMillis() - debut;
            System.out.println("Ingestion terminee en " + (duree/1000.0) + "s\n");

            // ETAPE 5: CREATION ASSISTANT RAG
            System.out.println("[5/5] CREATION DE L'ASSISTANT");
            Assistant assistant = AiServices.builder(Assistant.class)
                    .chatModel(chatModel)
                    .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                    .contentRetriever(EmbeddingStoreContentRetriever.builder()
                            .embeddingStore(embeddingStore)
                            .embeddingModel(embeddingModel)
                            .maxResults(15)
                            .minScore(0.6)
                            .build())
                    .build();
            System.out.println("Assistant pret!\n");

            // MODE CONVERSATIONNEL
            System.out.println("=== MODE CONVERSATIONNEL ===");
            System.out.println("Commandes:");
            System.out.println("  - Tapez votre question");
            System.out.println("  - 'quiz' pour generer un QCM");
            System.out.println("  - 'quit' pour quitter\n");

            Scanner scanner = new Scanner(System.in);
            int numeroQuestion = 0;

            while (true) {
                System.out.print("\n[Q" + (++numeroQuestion) + "] Vous > ");
                String question = scanner.nextLine().trim();

                if (question.isEmpty()) continue;
                if (question.equalsIgnoreCase("quit") || question.equalsIgnoreCase("exit")) {
                    System.out.println("\nMerci d'avoir utilise l'assistant!");
                    break;
                }

                if (question.equalsIgnoreCase("quiz")) {
                    question = "Genere un quiz de 5 questions QCM sur le Machine Learning. Pour chaque question, propose 4 reponses (A,B,C,D). Indique les bonnes reponses a la fin.";
                }

                try {
                    long start = System.currentTimeMillis();
                    String reponse = assistant.chat(question);
                    long time = System.currentTimeMillis() - start;

                    System.out.println("\n[Assistant] >");
                    System.out.println(reponse);
                    System.out.println("\n(Temps: " + time + " ms)");

                } catch (Exception e) {
                    System.err.println("\nERREUR: " + e.getMessage());
                }
            }

            scanner.close();

        } catch (Exception e) {
            System.err.println("\nERREUR FATALE: " + e.getMessage());
            e.printStackTrace();
        }
    }
}