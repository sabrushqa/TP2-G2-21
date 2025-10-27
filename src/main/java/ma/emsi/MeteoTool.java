package ma.emsi;

import dev.langchain4j.agent.tool.Tool;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Outil fournissant la météo actuelle d'une ville via l'API wttr.in.
 */
public class MeteoTool {

    /**
     * Cette annotation @Tool est essentielle. Son contenu (la description)
     * doit être clair pour que le LLM sache dans quels cas l'utiliser.
     * Le paramètre 'ville' sera extrait de la question de l'utilisateur.
     * * @param ville Le nom de la ville dont on souhaite connaître la météo.
     * @return Une chaîne de caractères contenant la météo formatée (ex: "Paris: 🌦 +14°C").
     */
    @Tool("Donne la météo actuelle dans une ville en utilisant l'API wttr.in")
    public String meteo(String ville) {
        try {
            // Utilisation du format=3 pour une réponse concise en ligne
            String urlString = "https://wttr.in/" + ville + "?format=3";
            URL url = new URL(urlString);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // Lecture de la réponse (qui est une seule ligne avec format=3)
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                return reader.readLine();
            }
        } catch (Exception e) {
            // En cas d'erreur (ville inconnue, problème réseau, etc.)
            return "Impossible d'obtenir la météo pour la ville : " + ville;
        }
    }
}
