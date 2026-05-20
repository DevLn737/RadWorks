package dev.radworks.radiation;

import dev.radworks.config.RadWorksConfig;
import dev.radworks.diagnostics.NestedContainerDiagnostics;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

public final class NestedContainerExtractor {
    private NestedContainerExtractor() {
    }

    public static List<ExtractedStack> expand(
            ItemStack rootStack,
            String sourcePath,
            NestedContainerDiagnostics.Builder diagnostics) {
        if (rootStack == null || rootStack.isEmpty()) {
            return List.of();
        }

        ResourceLocation rootId = BuiltInRegistries.ITEM.getKey(rootStack.getItem());
        List<ExtractedStack> extracted = new ArrayList<>();
        extracted.add(new ExtractedStack(
                rootId,
                rootStack.getCount(),
                false,
                0,
                null,
                sourcePath,
                "direct_stack"));

        if (!RadWorksConfig.nestedContainersEnabled()) {
            return List.copyOf(extracted);
        }

        ExtractionState state = new ExtractionState(
                RadWorksConfig.nestedContainerMaxDepth(),
                RadWorksConfig.nestedContainerMaxItemsPerSource(),
                diagnostics);
        expandChildren(rootStack, sourcePath, 0, extracted, state);
        return List.copyOf(extracted);
    }

    private static void expandChildren(
            ItemStack containerStack,
            String containerPath,
            int depth,
            List<ExtractedStack> extracted,
            ExtractionState state) {
        if (containerStack == null || containerStack.isEmpty()) {
            return;
        }

        ResourceLocation containerItemId = BuiltInRegistries.ITEM.getKey(containerStack.getItem());
        ContainerView view = tryExtractChildren(containerStack, containerItemId, containerPath, state.diagnostics());
        if (view == null) {
            return;
        }

        if (depth >= state.maxDepth()) {
            state.diagnostics().nestedDepthLimitHit();
            state.diagnostics().sample(
                    containerPath,
                    containerItemId,
                    containerPath,
                    depth,
                    view.extractionMode(),
                    "depth_limit_reached",
                    null,
                    null);
            return;
        }

        int index = 0;
        for (ItemStack childStack : view.children()) {
            if (state.extractedItems() >= state.maxItemsPerSource()) {
                state.diagnostics().nestedItemLimitHit();
                state.diagnostics().sample(
                        containerPath,
                        containerItemId,
                        containerPath,
                        depth + 1,
                        view.extractionMode(),
                        "item_limit_reached",
                        null,
                        null);
                return;
            }
            if (childStack == null || childStack.isEmpty()) {
                index++;
                continue;
            }

            ResourceLocation childItemId = BuiltInRegistries.ITEM.getKey(childStack.getItem());
            int childCount = childStack.getCount();
            String childPath = switch (view.extractionMode()) {
                case "bundle_contents" -> containerPath + ".item[" + index + "]";
                case "data_component_container" -> containerPath + ".slot[" + index + "]";
                default -> containerPath + ".nested[" + index + "]";
            };
            extracted.add(new ExtractedStack(
                    childItemId,
                    childCount,
                    true,
                    depth + 1,
                    containerItemId,
                    childPath,
                    view.extractionMode()));
            state.extractedItems++;
            state.diagnostics().nestedStackExtracted();
            state.diagnostics().sample(
                    containerPath,
                    containerItemId,
                    childPath,
                    depth + 1,
                    view.extractionMode(),
                    null,
                    childItemId,
                    childCount);
            expandChildren(childStack, childPath, depth + 1, extracted, state);
            index++;
        }
    }

    private static ContainerView tryExtractChildren(
            ItemStack stack,
            ResourceLocation containerItemId,
            String containerPath,
            NestedContainerDiagnostics.Builder diagnostics) {
        diagnostics.nestedContainerChecked();

        try {
            ItemContainerContents container = stack.get(DataComponents.CONTAINER);
            if (container != null) {
                List<ItemStack> children = new ArrayList<>();
                for (ItemStack child : container.nonEmptyItemsCopy()) {
                    children.add(child);
                }
                diagnostics.nestedContainerSupported();
                if (children.isEmpty()) {
                    diagnostics.sample(
                            containerPath,
                            containerItemId,
                            containerPath,
                            null,
                            "data_component_container",
                            "empty_nested_container",
                            null,
                            null);
                    return null;
                }
                return new ContainerView(children, "data_component_container");
            }

            BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
            if (bundle != null) {
                List<ItemStack> children = new ArrayList<>();
                for (ItemStack child : bundle.itemsCopy()) {
                    children.add(child);
                }
                diagnostics.nestedContainerSupported();
                if (children.isEmpty()) {
                    diagnostics.sample(
                            containerPath,
                            containerItemId,
                            containerPath,
                            null,
                            "bundle_contents",
                            "empty_nested_container",
                            null,
                            null);
                    return null;
                }
                return new ContainerView(children, "bundle_contents");
            }

            if (looksContainerLike(containerItemId)) {
                diagnostics.nestedContainerUnsupported();
                diagnostics.sample(
                        containerPath,
                        containerItemId,
                        containerPath,
                        null,
                        "unsupported_container_format",
                        "unsupported_container_format",
                        null,
                        null);
            }
            return null;
        } catch (RuntimeException exception) {
            diagnostics.nestedMalformedContainer();
            diagnostics.sample(
                    containerPath,
                    containerItemId,
                    containerPath,
                    null,
                    "malformed_container_contents",
                    "malformed_container_contents",
                    null,
                    null);
            return null;
        }
    }

    static boolean looksContainerLike(ResourceLocation itemId) {
        String path = itemId.getPath().toLowerCase(Locale.ROOT);
        return path.contains("shulker")
                || path.contains("bundle")
                || path.contains("backpack")
                || path.contains("toolbox")
                || path.contains("box")
                || path.contains("chest")
                || path.contains("crate")
                || path.contains("pouch");
    }

    public record ExtractedStack(
            ResourceLocation itemId,
            int count,
            boolean nested,
            int nestedDepth,
            ResourceLocation containerItemId,
            String containerPath,
            String extractionMode) {
    }

    private record ContainerView(List<ItemStack> children, String extractionMode) {
    }

    private static final class ExtractionState {
        private final int maxDepth;
        private final int maxItemsPerSource;
        private final NestedContainerDiagnostics.Builder diagnostics;
        private int extractedItems;

        private ExtractionState(
                int maxDepth,
                int maxItemsPerSource,
                NestedContainerDiagnostics.Builder diagnostics) {
            this.maxDepth = maxDepth;
            this.maxItemsPerSource = maxItemsPerSource;
            this.diagnostics = diagnostics;
        }

        private int maxDepth() {
            return maxDepth;
        }

        private int maxItemsPerSource() {
            return maxItemsPerSource;
        }

        private int extractedItems() {
            return extractedItems;
        }

        private NestedContainerDiagnostics.Builder diagnostics() {
            return diagnostics;
        }
    }
}
