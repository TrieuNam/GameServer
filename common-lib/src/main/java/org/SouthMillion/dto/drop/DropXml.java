package org.SouthMillion.dto.drop;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.List;

@Data
@JacksonXmlRootElement(localName = "drop")
public class DropXml {
    @JacksonXmlProperty(localName = "drop_id")
    private int dropId;

    @JacksonXmlProperty(localName = "drop_item_prob_list")
    private DropItemProbList dropItemProbList;

    @Data
    public static class DropItemProbList {
        @JacksonXmlProperty(localName = "drop_item_prob")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<DropItemProb> items;
    }

    @Data
    public static class DropItemProb {
        @JacksonXmlProperty(localName = "item_id")   private int itemId;
        @JacksonXmlProperty(localName = "is_bind")   private int isBind;
        @JacksonXmlProperty(localName = "prob")      private int prob;
        @JacksonXmlProperty(localName = "num")       private int num;
        @JacksonXmlProperty(localName = "broadcast") private int broadcast;
    }
}