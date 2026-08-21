package artframework.component;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * Loads restricted LML (XML) into {@link UiNode} trees. Pure Java; no LibGDX.
 * External entities / DTDs are disabled.
 */
public final class LmlUiNodeLoader {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private static final Set<String> STRUCT_ATTRS =
            new HashSet<String>(
                    Arrays.asList(
                            "id",
                            "type",
                            "ref",
                            "signals",
                            "width",
                            "height",
                            "minWidth",
                            "minHeight",
                            "min-w",
                            "min-h",
                            "minW",
                            "minH",
                            "pad",
                            "gap",
                            "grow",
                            "align",
                            "stretchRatio",
                            "stretch-ratio",
                            "ratio",
                            "sizeFlagsH",
                            "sizeFlagsV",
                            "size-flags-h",
                            "size-flags-v"));

    private static final Set<String> LAYOUT_ATTRS =
            new HashSet<String>(
                    Arrays.asList(
                            "width",
                            "height",
                            "minWidth",
                            "minHeight",
                            "min-w",
                            "min-h",
                            "minW",
                            "minH",
                            "pad",
                            "gap",
                            "grow",
                            "align",
                            "stretchRatio",
                            "stretch-ratio",
                            "ratio",
                            "sizeFlagsH",
                            "sizeFlagsV",
                            "size-flags-h",
                            "size-flags-v"));

    private LmlUiNodeLoader() {}

    public static UiNode loadClasspath(String resource) {
        if (resource == null || resource.isEmpty()) {
            throw new IllegalArgumentException("layout resource required");
        }
        InputStream in = LmlUiNodeLoader.class.getClassLoader().getResourceAsStream(resource);
        if (in == null) {
            throw new IllegalArgumentException("layout not found: " + resource);
        }
        try {
            return parse(readFully(in));
        } catch (IOException e) {
            throw new IllegalArgumentException("layout read failed: " + resource, e);
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static UiNode parse(String lml) {
        if (lml == null || lml.trim().isEmpty()) {
            throw new IllegalArgumentException("layout lml required");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setValidating(false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            try {
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            } catch (Exception ignored) {
            }
            try {
                factory.setFeature(
                        "http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature(
                        "http://xml.org/sax/features/external-parameter-entities", false);
            } catch (Exception ignored) {
            }
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver(
                    new org.xml.sax.EntityResolver() {
                        @Override
                        public org.xml.sax.InputSource resolveEntity(String publicId, String systemId)
                                throws org.xml.sax.SAXException {
                            throw new org.xml.sax.SAXException("external entities disabled");
                        }
                    });
            builder.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) throws SAXParseException {
                    throw exception;
                }

                @Override
                public void error(SAXParseException exception) throws SAXParseException {
                    throw exception;
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXParseException {
                    throw exception;
                }
            });
            Document doc =
                    builder.parse(new ByteArrayInputStream(lml.getBytes(UTF_8)));
            Element root = doc.getDocumentElement();
            if (root == null) {
                throw new IllegalArgumentException("layout root required");
            }
            return toNode(root, true);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("lml parse failed: " + e.getMessage(), e);
        }
    }

    private static UiNode toNode(Element el, boolean requireInteractiveSignals) {
        String tag = el.getTagName();
        String type;
        if ("node".equals(tag)) {
            type = attr(el, "type");
            if (type == null || type.isEmpty()) {
                throw new IllegalArgumentException("node type attribute required");
            }
        } else if ("effect".equals(tag)) {
            throw new IllegalArgumentException("effect must be nested under a host node");
        } else {
            type = tag;
        }
        if (!UiNodeRegistry.global().contains(type)) {
            throw new IllegalArgumentException("unknown layout type: " + type);
        }

        UiNode.Builder b = UiNode.of(type).id(attr(el, "id"));
        String ref = attr(el, "ref");
        if (ref != null) {
            b.ref(ref);
        }

        Map<String, Object> props = new LinkedHashMap<String, Object>();
        NamedNodeMap attrs = el.getAttributes();
        if (attrs != null) {
            for (int i = 0; i < attrs.getLength(); i++) {
                Node a = attrs.item(i);
                String name = a.getNodeName();
                if (STRUCT_ATTRS.contains(name) || "type".equals(name)) {
                    continue;
                }
                props.put(camelProp(name), coerce(a.getNodeValue()));
            }
        }
        if (UiTypes.WINDOW.equals(type) && !props.containsKey("title")) {
            String title = attr(el, "title");
            if (title != null) {
                props.put("title", title);
            }
        }
        if (UiTypes.SLOT.equals(type) && !props.containsKey("name")) {
            String name = attr(el, "name");
            if (name != null) {
                props.put("name", name);
            }
        }
        b.props(props);
        b.layout(parseLayout(el));

        List<EffectDecl> effects = new ArrayList<EffectDecl>();
        List<UiNode> children = new ArrayList<UiNode>();
        Map<String, List<UiNode>> slots = new LinkedHashMap<String, List<UiNode>>();
        NodeList kids = el.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            Node kn = kids.item(i);
            if (kn.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element child = (Element) kn;
            String childTag = child.getTagName();
            if ("effect".equals(childTag)) {
                effects.add(parseEffect(child));
            } else if ("slot".equals(childTag) && UiTypes.REF.equals(type)) {
                String slotName = attr(child, "name");
                if (slotName == null || slotName.isEmpty()) {
                    slotName = "default";
                }
                List<UiNode> slotNodes = new ArrayList<UiNode>();
                NodeList slotKids = child.getChildNodes();
                for (int j = 0; j < slotKids.getLength(); j++) {
                    Node sn = slotKids.item(j);
                    if (sn.getNodeType() == Node.ELEMENT_NODE) {
                        slotNodes.add(toNode((Element) sn, requireInteractiveSignals));
                    }
                }
                slots.put(slotName, slotNodes);
            } else {
                children.add(toNode(child, requireInteractiveSignals));
            }
        }
        if (UiTypes.GLASS.equals(type) && effects.isEmpty()) {
            Map<String, Object> gp = new LinkedHashMap<String, Object>();
            if (attr(el, "radius") != null) {
                gp.put("radius", coerce(attr(el, "radius")));
            }
            if (attr(el, "tint") != null) {
                gp.put("tint", coerce(attr(el, "tint")));
            }
            if (attr(el, "alpha") != null) {
                gp.put("alpha", coerce(attr(el, "alpha")));
            }
            effects.add(new EffectDecl("glass", gp));
        }
        b.effects(effects);
        b.children(children);
        b.slots(slots);

        String signalsAttr = attr(el, "signals");
        if (signalsAttr != null) {
            b.signals(splitSignals(signalsAttr));
        } else if (requireInteractiveSignals && needsExplicitSignals(type)) {
            throw new IllegalArgumentException(
                    "LML interactive node requires signals attribute: type="
                            + type
                            + " id="
                            + attr(el, "id"));
        }

        if (UiTypes.WINDOW.equals(type)) {
            String title = props.containsKey("title") ? String.valueOf(props.get("title")) : "";
            if (title.isEmpty()) {
                throw new IllegalArgumentException("window title required");
            }
        }
        return b.build();
    }

    private static boolean needsExplicitSignals(String type) {
        return UiTypes.BUTTON.equals(type)
                || UiTypes.SLIDER.equals(type)
                || UiTypes.HITAREA.equals(type)
                || UiTypes.TEXTFIELD.equals(type)
                || UiTypes.CHECKBOX.equals(type)
                || UiTypes.PROGRESS.equals(type);
    }

    private static EffectDecl parseEffect(Element el) {
        String id = attr(el, "id");
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("effect id required");
        }
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        NamedNodeMap attrs = el.getAttributes();
        if (attrs != null) {
            for (int i = 0; i < attrs.getLength(); i++) {
                Node a = attrs.item(i);
                if ("id".equals(a.getNodeName())) {
                    continue;
                }
                params.put(camelProp(a.getNodeName()), coerce(a.getNodeValue()));
            }
        }
        return new EffectDecl(id, params);
    }

    private static LayoutSpec parseLayout(Element el) {
        float width = floatAttr(el, "width", 0f);
        float height = floatAttr(el, "height", 0f);
        float minWidth = floatAttr(el, "minWidth", floatAttr(el, "min-w", floatAttr(el, "minW", 0f)));
        float minHeight =
                floatAttr(el, "minHeight", floatAttr(el, "min-h", floatAttr(el, "minH", 0f)));
        float pad = floatAttr(el, "pad", 0f);
        float gap = floatAttr(el, "gap", 0f);
        boolean grow = boolAttr(el, "grow", false);
        float stretchRatio =
                floatAttr(
                        el,
                        "stretchRatio",
                        floatAttr(el, "stretch-ratio", floatAttr(el, "ratio", 1f)));
        String align = attr(el, "align");
        Integer flagsH = null;
        Integer flagsV = null;
        if (attr(el, "sizeFlagsH") != null || attr(el, "size-flags-h") != null) {
            flagsH =
                    Integer.valueOf(
                            (int)
                                    floatAttr(
                                            el,
                                            "sizeFlagsH",
                                            floatAttr(el, "size-flags-h", SizeFlags.DEFAULT)));
        }
        if (attr(el, "sizeFlagsV") != null || attr(el, "size-flags-v") != null) {
            flagsV =
                    Integer.valueOf(
                            (int)
                                    floatAttr(
                                            el,
                                            "sizeFlagsV",
                                            floatAttr(el, "size-flags-v", SizeFlags.DEFAULT)));
        }
        boolean anyLayout = false;
        NamedNodeMap attrs = el.getAttributes();
        if (attrs != null) {
            for (int i = 0; i < attrs.getLength(); i++) {
                if (LAYOUT_ATTRS.contains(attrs.item(i).getNodeName())) {
                    anyLayout = true;
                    break;
                }
            }
        }
        if (!anyLayout) {
            return LayoutSpec.EMPTY;
        }
        int sfH = flagsH != null ? flagsH.intValue() : SizeFlags.fromGrow(grow);
        int sfV = flagsV != null ? flagsV.intValue() : SizeFlags.fromGrow(grow);
        return new LayoutSpec(
                width, height, minWidth, minHeight, pad, gap, sfH, sfV, stretchRatio, align);
    }

    private static List<String> splitSignals(String raw) {
        List<String> out = new ArrayList<String>();
        if (raw == null) {
            return out;
        }
        String[] parts = raw.split(",");
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    private static String attr(Element el, String name) {
        if (!el.hasAttribute(name)) {
            return null;
        }
        return el.getAttribute(name);
    }

    private static float floatAttr(Element el, String name, float def) {
        String v = attr(el, name);
        if (v == null || v.isEmpty()) {
            return def;
        }
        try {
            return Float.parseFloat(v.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid float attr " + name + ": " + v);
        }
    }

    private static boolean boolAttr(Element el, String name, boolean def) {
        String v = attr(el, name);
        if (v == null || v.isEmpty()) {
            return def;
        }
        return "true".equalsIgnoreCase(v) || "1".equals(v);
    }

    private static Object coerce(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if ("true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s)) {
            return Boolean.valueOf(s);
        }
        try {
            if (s.indexOf('.') >= 0) {
                return Float.valueOf(s);
            }
            return Integer.valueOf(s);
        } catch (NumberFormatException ignored) {
            return s;
        }
    }

    private static String camelProp(String name) {
        if (name == null || name.indexOf('-') < 0) {
            return name;
        }
        StringBuilder sb = new StringBuilder();
        boolean up = false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '-') {
                up = true;
            } else if (up) {
                sb.append(Character.toUpperCase(c));
                up = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String readFully(InputStream in) throws IOException {
        byte[] buf = new byte[4096];
        StringBuilder sb = new StringBuilder();
        int n;
        while ((n = in.read(buf)) >= 0) {
            sb.append(new String(buf, 0, n, UTF_8));
        }
        return sb.toString();
    }
}
