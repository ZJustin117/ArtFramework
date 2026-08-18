package artframework.component;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class ImmutableUiValueTest {

    @Test
    public void deepCopiesNestedDeclarationValues() {
        List<Object> nested = new ArrayList<Object>(Arrays.<Object>asList("one"));
        Map<String, Object> input = new LinkedHashMap<String, Object>();
        input.put("items", nested);

        @SuppressWarnings("unchecked")
        Map<String, Object> output = (Map<String, Object>) ImmutableUiValue.copy(input);
        nested.add("two");

        assertEquals(Arrays.<Object>asList("one"), output.get("items"));
        try {
            ((List<Object>) output.get("items")).add("three");
            fail("nested list must be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsHostObjects() {
        ImmutableUiValue.copy(new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonStringMapKeys() {
        Map<Object, Object> input = new LinkedHashMap<Object, Object>();
        input.put(Integer.valueOf(1), "value");
        ImmutableUiValue.copy(input);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nodeBuilderRejectsHostProperty() {
        UiNode.of(UiTypes.LABEL).prop("host", new Object()).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsCyclicMap() {
        Map<String, Object> input = new LinkedHashMap<String, Object>();
        input.put("self", input);
        ImmutableUiValue.copy(input);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsCyclicList() {
        List<Object> input = new ArrayList<Object>();
        input.add(input);
        ImmutableUiValue.copy(input);
    }

    @Test
    public void effectParamsUseTheSameDeepCopySchema() {
        Map<String, Object> nested = new LinkedHashMap<String, Object>();
        nested.put("colors", new ArrayList<Object>(Arrays.<Object>asList("blue")));
        EffectDecl effect = new EffectDecl("lightwave", nested);
        ((List<Object>) nested.get("colors")).add("red");

        @SuppressWarnings("unchecked")
        List<Object> colors = (List<Object>) effect.params.get("colors");
        assertEquals(Arrays.<Object>asList("blue"), colors);
        try {
            colors.add("green");
            fail("effect params must be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void effectParamsRejectHostObjects() {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("host", new Object());
        new EffectDecl("lightwave", params);
    }

    @Test
    public void builderAndPropertyOverlayCopyNestedValues() {
        Map<String, Object> source = new LinkedHashMap<String, Object>();
        source.put("nested", new ArrayList<Object>(Arrays.<Object>asList("one")));
        UiNode node = UiNode.of(UiTypes.LABEL).props(source).build();
        source.clear();
        UiNode rebuilt = node.toBuilder().build();
        artframework.presentation.NodePropertiesComponent properties =
                new artframework.presentation.NodePropertiesComponent(rebuilt.props)
                        .with("extra", Arrays.<Object>asList("two"));

        assertEquals(Arrays.<Object>asList("one"), rebuilt.props.get("nested"));
        assertEquals(Arrays.<Object>asList("two"), properties.get("extra"));
        assertNull(source.get("nested"));
    }

    @Test
    public void sharedAcyclicValuesAreCopiedPerBranchAndRemainImmutable() {
        Map<String, Object> shared = new LinkedHashMap<String, Object>();
        shared.put("items", new ArrayList<Object>(Arrays.<Object>asList("one")));
        Map<String, Object> source = new LinkedHashMap<String, Object>();
        source.put("left", shared);
        source.put("right", Arrays.<Object>asList(shared));

        @SuppressWarnings("unchecked")
        Map<String, Object> copied = (Map<String, Object>) ImmutableUiValue.copy(source);
        shared.put("later", "mutated");
        ((List<Object>) shared.get("items")).add("two");

        @SuppressWarnings("unchecked")
        Map<String, Object> left = (Map<String, Object>) copied.get("left");
        @SuppressWarnings("unchecked")
        Map<String, Object> right = (Map<String, Object>) ((List<Object>) copied.get("right")).get(0);
        assertEquals(Arrays.<Object>asList("one"), left.get("items"));
        assertEquals(Arrays.<Object>asList("one"), right.get("items"));
        assertNull(left.get("later"));
        assertNull(right.get("later"));
        try {
            left.put("blocked", "value");
            fail("nested map must be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
        try {
            ((List<Object>) right.get("items")).add("blocked");
            fail("nested list must be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void failedCyclicCopyDoesNotPoisonLaterCopies() {
        Map<String, Object> cyclic = new LinkedHashMap<String, Object>();
        cyclic.put("self", cyclic);
        try {
            ImmutableUiValue.copy(cyclic);
            fail("cycle must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        Map<String, Object> valid = new LinkedHashMap<String, Object>();
        valid.put("value", Arrays.<Object>asList("ok"));
        assertEquals(valid, ImmutableUiValue.copy(valid));
    }
}
