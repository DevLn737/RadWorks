package dev.radworks.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class CreateTransientCarrierExtractorTest {
    @Test
    void parsesPlacardItemPayload() {
        CompoundTag root = new CompoundTag();
        CompoundTag item = new CompoundTag();
        item.putString("id", "createnuclear:raw_uranium");
        item.putInt("count", 1);
        root.put("Item", item);

        var parsed = CreateTransientCarrierExtraction.parseItemAtRoot(root, "Item");
        assertTrue(parsed.isPresent());
        assertEquals("createnuclear:raw_uranium", parsed.get().id().toString());
        assertEquals(1, parsed.get().count());
    }

    @Test
    void parsesMechanicalArmHeldItemPayload() {
        CompoundTag root = new CompoundTag();
        CompoundTag held = new CompoundTag();
        held.putString("id", "createnuclear:raw_uranium");
        held.putInt("count", 64);
        root.put("HeldItem", held);

        var parsed = CreateTransientCarrierExtraction.parseItemAtRoot(root, "HeldItem");
        assertTrue(parsed.isPresent());
        assertEquals("createnuclear:raw_uranium", parsed.get().id().toString());
        assertEquals(64, parsed.get().count());
    }

    @Test
    void parsesFluidPipeSideFlowPayload() {
        CompoundTag root = new CompoundTag();
        CompoundTag east = new CompoundTag();
        CompoundTag flow = new CompoundTag();
        CompoundTag fluid = new CompoundTag();
        fluid.putString("id", "createnuclear:uranium");
        fluid.putInt("amount", 1);
        flow.put("Fluid", fluid);
        east.put("Flow", flow);
        root.put("east", east);

        var parsed = CreateTransientCarrierExtraction.parseFluidAtSideFlow(root, "east");
        assertTrue(parsed.isPresent());
        assertEquals("createnuclear:uranium", parsed.get().id().toString());
        assertEquals(1, parsed.get().amountMb());
    }

    @Test
    void parsesFluidPipeSideFlowWithCaseVariants() {
        CompoundTag root = new CompoundTag();
        CompoundTag east = new CompoundTag();
        CompoundTag flow = new CompoundTag();
        CompoundTag fluid = new CompoundTag();
        fluid.putString("Id", "createnuclear:uranium");
        fluid.putInt("Amount", 1);
        flow.put("fluid", fluid);
        east.put("flow", flow);
        root.put("East", east);

        var parsed = CreateTransientCarrierExtraction.parseFluidAtSideFlow(root, "east");
        assertTrue(parsed.isPresent());
        assertEquals("createnuclear:uranium", parsed.get().id().toString());
        assertEquals(1, parsed.get().amountMb());
    }

    @Test
    void parsesFluidAmountFromString() {
        CompoundTag root = new CompoundTag();
        CompoundTag east = new CompoundTag();
        CompoundTag flow = new CompoundTag();
        CompoundTag fluid = new CompoundTag();
        fluid.putString("id", "createnuclear:uranium");
        fluid.putString("amount", "1");
        flow.put("Fluid", fluid);
        east.put("Flow", flow);
        root.put("east", east);

        var parsed = CreateTransientCarrierExtraction.parseFluidAtSideFlow(root, "east");
        assertTrue(parsed.isPresent());
        assertEquals(1, parsed.get().amountMb());
    }

    @Test
    void malformedPayloadDoesNotParse() {
        CompoundTag root = new CompoundTag();
        CompoundTag item = new CompoundTag();
        item.putInt("count", 1);
        root.put("Item", item);

        assertTrue(CreateTransientCarrierExtraction.parseItemAtRoot(root, "Item").isEmpty());
    }

    @Test
    void malformedFluidReportsDetailedReason() {
        CompoundTag root = new CompoundTag();
        CompoundTag east = new CompoundTag();
        CompoundTag flow = new CompoundTag();
        CompoundTag fluid = new CompoundTag();
        fluid.putString("id", "createnuclear:uranium");
        flow.put("Fluid", fluid);
        east.put("Flow", flow);
        root.put("east", east);

        var parsed = CreateTransientCarrierExtraction.parseFluidAtSideFlowDetailed(root, "east");
        assertEquals(CreateTransientCarrierExtraction.FluidParseStatus.AMOUNT_MISSING, parsed.status());
        assertTrue(parsed.payload().isEmpty());
        assertEquals("createnuclear:uranium", parsed.parsedFluidId().toString());
    }
}
