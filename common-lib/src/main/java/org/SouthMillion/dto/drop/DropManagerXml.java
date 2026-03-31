package org.SouthMillion.dto.drop;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.List;

@Data
@JacksonXmlRootElement(localName = "dropmanager")
public class DropManagerXml {
    @JacksonXmlProperty(localName = "path")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<String> path;
}