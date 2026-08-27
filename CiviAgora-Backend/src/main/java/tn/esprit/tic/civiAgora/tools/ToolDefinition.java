package tn.esprit.tic.civiAgora.tools;

import java.util.Map;

/**
 * Interface for chatbot tools.
 * Each tool = one action the LLM can call (query data, create records, etc.)
 * All tools are auto-discovered by Spring via @Component.
 */
public interface ToolDefinition {

    enum Operation {
        READ,
        WRITE
    }

    String getName();

    String getDescription();

    Map<String, Object> getInputSchema();

    default Operation getOperation() {
        return Operation.READ;
    }

    default String getRequiredModule() {
        return null;
    }

    default boolean requiresConfirmation() {
        return false;
    }

    /**
     * Execute the tool.
     * @param input           parsed arguments from the LLM
     * @param context         trusted identity and tenant resolved by the backend
     * @return JSON string result that gets fed back to the LLM
     */
    String execute(Map<String, Object> input, ToolExecutionContext context) throws Exception;
}
