// Copyright 2024 Atakku <https://atakku.dev>
//
// This project is dual licensed under MIT and Apache.

package rs.neko.smp.worldtest.server;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.server.command.CommandManager.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class WorldTest implements ModInitializer {
  public static final String MOD_ID = "nsmp-worldtest";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  @Override
  public void onInitialize() {
    LOGGER.info("Initializing NSMP WorldTest");

    CommandRegistrationCallback.EVENT.register((d, r, e) -> {
      d.register(literal("worldtest").requires(s -> s.hasPermissionLevel(2)).executes(ctx -> {
        CompletableFuture.supplyAsync(() -> {
          worldTest(ctx.getSource().getWorld());
          return 0;
        }, Util.getIoWorkerExecutor());
        return 1;
      }));
    });
  }

  public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  public static final int side = 8192;
  public static final int threads = 16;

  public static void worldTest(World w) {
    BiomeAccess ba = w.getBiomeAccess();
    Registry<Biome> br = w.getRegistryManager().get(RegistryKeys.BIOME);
    Object2IntOpenHashMap<Identifier> bc = new Object2IntOpenHashMap<>();
    br.getEntrySet().forEach(e -> bc.put(e.getKey().getValue(), 0));

    for (int t = 0; t < threads; t++) {
      int thread = t;
      Util.getIoWorkerExecutor().execute(() -> {
        for (int x = 0; x < side / 2; x++) {
          if (x % threads != thread)
            continue;
          for (int z = 0; z < side / 2; z++) {
            Identifier id = ba.getBiome(new BlockPos(x * 4 - side, 256, z * 4  - side)).getKey().get().getValue();
            bc.put(id, bc.getInt(id) + 1);
          }
          System.out.println(x);
        }

        // TODO: await all threads instead
        try {
          Files.writeString(Path.of("./test.json"), gson.toJson(bc));
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      });
    }
  }
}
