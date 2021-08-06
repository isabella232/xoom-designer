package ${packageName};

import io.vlingo.xoom.actors.World;
import io.vlingo.xoom.actors.testkit.AccessSafely;
import io.vlingo.xoom.common.serialization.JsonSerialization;
import io.vlingo.xoom.lattice.model.projection.Projectable;
import io.vlingo.xoom.lattice.model.projection.Projection;
import io.vlingo.xoom.lattice.model.projection.TextProjectable;
import io.vlingo.xoom.lattice.model.stateful.StatefulTypeRegistry;
import io.vlingo.xoom.symbio.Metadata;
import io.vlingo.xoom.symbio.State.TextState;
import io.vlingo.xoom.symbio.store.dispatch.NoOpDispatcher;
import io.vlingo.xoom.symbio.store.state.StateStore;
import io.vlingo.xoom.symbio.store.state.inmemory.InMemoryStateStoreActor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

<#list imports as import>
import ${import.qualifiedClassName};
</#list>

public class ${projectionUnitTestName} {
  private Projection projection;
  private StateStore store;
  private Map<String,String> valueToProjectionId;
  private World world;

  @BeforeEach
  public void setUp() {
    world = World.startWithDefaults("test-state-store-projection");

    store = world.actorFor(StateStore.class, InMemoryStateStoreActor.class, Arrays.asList(new NoOpDispatcher()));

    projection = world.actorFor(Projection.class, ${projectionName}.class, store);

    StatefulTypeRegistry.registerAll(world, store, ${dataObjectName}.class);

    valueToProjectionId = new HashMap<>();
  }

  @Test
  public void testThatProjectionsProject() {
    final CountingProjectionControl control = new CountingProjectionControl();
    final AccessSafely access = control.afterCompleting(3);

    projection.projectWith(textWarble("1", 1), control);
    projection.projectWith(textWarble("2", 2), control);
    projection.projectWith(textWarble("3", 3), control);

    final Map<String,Integer> confirmations = access.readFrom("confirmations");

    assertEquals(3, confirmations.size());

    assertEquals(1, valueOfProjectionIdFor("1", confirmations));
    assertEquals(1, valueOfProjectionIdFor("2", confirmations));
    assertEquals(1, valueOfProjectionIdFor("3", confirmations));

		final CountingReadResultInterest interest = new CountingReadResultInterest();

		final AccessSafely accessInterest = interest.afterCompleting(3);
		assertNotNull(accessInterest.readFrom("warble", "1"));
  }

	@Test
	public void testThatProjectionsUpdate() {
		final CountingProjectionControl control = new CountingProjectionControl();

		final AccessSafely accessControl = control.afterCompleting(6);

		projection.projectWith(textWarble("1", 1), control);
		projection.projectWith(textWarble("2", 2), control);
		projection.projectWith(textWarble("3", 3), control);

		projection.projectWith(textWarble("1", 4), control);
		projection.projectWith(textWarble("2", 5), control);
		projection.projectWith(textWarble("3", 6), control);

		final Map<String,Integer> confirmations = accessControl.readFrom("confirmations");

		assertEquals(6, confirmations.size());

		assertEquals(1, valueOfProjectionIdFor("1", confirmations));
		assertEquals(1, valueOfProjectionIdFor("2", confirmations));
		assertEquals(1, valueOfProjectionIdFor("3", confirmations));

		final CountingReadResultInterest interest = new CountingReadResultInterest();

		final AccessSafely accessInterest = interest.afterCompleting(3);

		store.read("1", ${dataName}.class, interest);
		store.read("2", ${dataName}.class, interest);
		store.read("3", ${dataName}.class, interest);

		final ${dataName} warble1 = accessInterest.readFrom("warble", "1");
		assertNotNull(warble1);

		final ${dataName} warble2 = accessInterest.readFrom("warble", "2");
		assertNotNull(warble2);

		final ${dataName} warble3 = accessInterest.readFrom("warble", "3");
		assertNotNull(warble3);
	}

	@Test
	public void testThatProjectionsWriteStateBeforeHandlingNextEvent() {
		final CountingProjectionControl control = new CountingProjectionControl();

		final AccessSafely accessControl = control.afterCompleting(3);

		projection.projectWith(textWarble("1", 1), control);
		projection.projectWith(textWarble("1", 2), control);
		projection.projectWith(textWarble("1", 3), control);

		final Map<String,Integer> confirmations = accessControl.readFrom("confirmations");

		assertEquals(3, confirmations.size());

		final CountingReadResultInterest interest = new CountingReadResultInterest();

		final AccessSafely accessInterest = interest.afterCompleting(1);

		store.read("1", ${dataName}.class, interest);

		final ${dataName} warble = accessInterest.readFrom("warble", "1");
		assertNotNull(warble);
	}

	@Test
	public void testThatProjectionDoesNotRequireDiff() {
		final Projection projection = world.actorFor(Projection.class, ${projectionName}.class, store, true);

		final CountingProjectionControl control = new CountingProjectionControl();

		final AccessSafely accessControl = control.afterCompleting(2);

		projection.projectWith(textWarble("1", 1), control);
		projection.projectWith(textWarble("1", 0), control);

		final Map<String,Integer> confirmations = accessControl.readFrom("confirmations");

		assertEquals(2, confirmations.size());

		final CountingReadResultInterest interest = new CountingReadResultInterest();

		final AccessSafely accessInterest = interest.afterCompleting(1);

		store.read("1", ${dataName}.class, interest);

		final ${dataName} warble = accessInterest.readFrom("warble", "1");
		assertNotNull(warble);
	}

	@Test
	public void testThatProjectionDoesRequireDiff() {
		final Projection projection = world.actorFor(Projection.class, ${projectionName}.class, store, false);

		final CountingProjectionControl control = new CountingProjectionControl();

		final AccessSafely accessControl = control.afterCompleting(3);

		projection.projectWith(textWarble("1", 1_000), control);
		projection.projectWith(textWarble("1", 1_000), control); // forces previousData answer to not write
		projection.projectWith(textWarble("1", 3_000), control);

		final Map<String,Integer> confirmations = accessControl.readFrom("confirmations");

		assertEquals(3, confirmations.size());

		final CountingReadResultInterest interest = new CountingReadResultInterest();

		final AccessSafely accessInterest = interest.afterCompleting(1);

		store.read("1", ${dataName}.class, interest);

		final ${dataName} warble = accessInterest.readFrom("warble", "1");
		assertNotNull(warble);
}

  @AfterEach
  public void tearDown() {
    world.terminate();
  }

  private Projectable textWarble(final String id, final int value) {
    ${testCases?first.dataDeclarations?first}

    final TextState state = new TextState(id, ${dataName}.class, 1, JsonSerialization.serialized(firstData.to${dataName}()), 1, Metadata.withObject(firstData.to${dataName}()));

    final String valueText = Integer.toString(value);
    final String projectionId = UUID.randomUUID().toString();

    valueToProjectionId.put(valueText, projectionId);

    return new TextProjectable(state, Collections.emptyList(), projectionId);
  }

  private int valueOfProjectionIdFor(final String valueText, final Map<String,Integer> confirmations) {
    return confirmations.get(valueToProjectionId.get(valueText));
  }

}
