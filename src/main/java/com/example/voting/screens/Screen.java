package com.example.voting.screens;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * The message shapes the mobile client understands, exactly as the brief's annex defines them.
 *
 * <p>The field names on the wire are Portuguese because they are the client's contract, not ours;
 * the Java names stay English like the rest of the code, and {@code @JsonProperty} bridges the two.
 * Renaming any of these fields breaks every installed copy of the application.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record Screen(
        @JsonProperty("tipo") ScreenType type,
        @JsonProperty("titulo") String title,
        @JsonProperty("itens") List<?> items,
        @JsonProperty("botaoOk") Button okButton,
        @JsonProperty("botaoCancelar") Button cancelButton) {

    static Screen form(String title, List<FormItem> items, Button okButton, Button cancelButton) {
        return new Screen(ScreenType.FORMULARIO, title, items, okButton, cancelButton);
    }

    static Screen selection(String title, List<SelectionItem> items) {
        return new Screen(ScreenType.SELECAO, title, items, null, null);
    }

    enum ScreenType {
        FORMULARIO,
        SELECAO
    }

    /** An action the client performs by POSTing {@code body} to {@code url}. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Button(
            @JsonProperty("texto") String text,
            @JsonProperty("url") String url,
            @JsonProperty("body") Map<String, Object> body) {

        static Button to(String text, String url) {
            return new Button(text, url, null);
        }
    }

    /** One option in a SELECAO screen. Choosing it POSTs {@code body} to {@code url}. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record SelectionItem(
            @JsonProperty("texto") String text,
            @JsonProperty("url") String url,
            @JsonProperty("body") Map<String, Object> body) {

        static SelectionItem to(String text, String url) {
            return new SelectionItem(text, url, null);
        }
    }

    /** One line of a FORMULARIO screen: either a piece of text or a field to fill in. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record FormItem(
            @JsonProperty("tipo") FormItemType type,
            @JsonProperty("id") String id,
            @JsonProperty("titulo") String title,
            @JsonProperty("texto") String text,
            @JsonProperty("valor") Object value) {

        static FormItem text(String text) {
            return new FormItem(FormItemType.TEXTO, null, null, text, null);
        }

        static FormItem textInput(String id, String title, String value) {
            return new FormItem(FormItemType.INPUT_TEXTO, id, title, null, value);
        }

        enum FormItemType {
            TEXTO,
            INPUT_TEXTO,
            INPUT_NUMERO,
            INPUT_DATA
        }
    }
}
