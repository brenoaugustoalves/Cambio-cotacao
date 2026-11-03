package br.com.cambioclaro.app.economy.presentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable text and links for a short official-news list. */
public final class NewsDisplay {
    public static final class Item {
        public final String text,url;
        Item(String text,String url){this.text=text;this.url=url;}
    }
    public final String status;
    public final List<Item> items;
    NewsDisplay(String status,List<Item> items){this.status=status;this.items=Collections.unmodifiableList(new ArrayList<>(items));}
}
