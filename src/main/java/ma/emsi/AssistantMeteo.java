package ma.emsi;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Interface de l'assistant IA spécialisé dans la météo.
 * Le @SystemMessage instruit le modèle sur son rôle et l'utilisation des outils.
 */
public interface AssistantMeteo {

    @SystemMessage("Tu es un assistant météo. Si une question concerne la météo, le temps qu'il fait, les conditions climatiques, ou si un parapluie est nécessaire, utilise l'outil disponible.")
    String repondre(@UserMessage String question);
}
