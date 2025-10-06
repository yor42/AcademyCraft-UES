package cn.academy;

import cn.lambdalib2.crafting.CustomMappingHelper;
import cn.lambdalib2.crafting.RecipeRegistry;
import cn.lambdalib2.registry.RegistryMod;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.util.Debug;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map.Entry;

import static cn.academy.AcademyCraft.MODID;
import static cn.academy.AcademyCraft.NAME;
import static cn.academy.util.ResourcepackUtil.generateTexturePack;

/**
 * Academy Craft Mod Main Class
 * 
 * @author acaly, WeathFolD, KS
 *
 */
@Mod(modid = MODID, name = NAME, version = AcademyCraft.VERSION, dependencies = "required-after:"+Tags.LAMBDALIB_MOD_ID+"@"+Tags.LAMBDA_LIB_VERSION)
@RegistryMod(rootPackage = Tags.ROOT_PACKAGE, resourceDomain = AcademyCraft.MODID)
public class AcademyCraft {

    @Instance("academy-craft")
    public static AcademyCraft INSTANCE;

    public static final String MODID = Tags.MOD_ID;
    public static final String NAME = Tags.MOD_NAME;
    public static final String VERSION = Tags.VERSION;


    public static final boolean DEBUG_MODE = FMLLaunchHandler.isDeobfuscatedEnvironment();

    public static final Logger log = LogManager.getLogger(NAME);

    static final String[] scripts = { "generic", "ability", "electromaster", "teleporter", "meltdowner",
            "generic_skills" };

    public static Configuration config;

    public static RecipeRegistry recipes;

    public static SimpleNetworkWrapper netHandler = NetworkRegistry.INSTANCE.newSimpleChannel("academy-network");

    public static CreativeTabs cct = new CreativeTabs(NAME) {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(ACItems.logo);
        }
    };

    @StateEventCallback(priority = 1)
    private static void preInit(FMLPreInitializationEvent event) {
        log.info("Starting AcademyCraft");
        log.info("Copyright (c) Lambda Innovation, 2013-");
        log.info("https://ac.li-dev.cn/");
        log.info("In memory of WeAthFoLD, Thank you for playing! -yor42");
        recipes = new RecipeRegistry();
        //OBJLoader.INSTANCE.addDomain(Main.MODID);

        config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        ACOreDict.InitOredicts();
        if(FMLCommonHandler.instance().getSide() == Side.CLIENT){
            generateTexturePack();
        }
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {

        // Load script, where names now are available
        recipes.addRecipeFromResourceLocation(new ResourceLocation("academy:recipes/default.recipe"));

        if (DEBUG_MODE) {
            Debug.log("|-------------------------------------------------------");
            Debug.log("| AC Recipe Name Mappings");
            Debug.log("|--------------------------|----------------------------");
            Debug.log(String.format("| %-25s| Object Name", "Recipe Name"));
            Debug.log("|--------------------------|----------------------------");
            for (Entry<String, Object> entry : recipes.getNameMappingForDebug().entrySet()) {
                Object obj = entry.getValue();
                String str1 = entry.getKey(), str2;
                if (obj instanceof Item) {
                    str2 = I18n.translateToLocal(((Item) obj).getTranslationKey() + ".name");
                } else if (obj instanceof Block) {
                    str2 = I18n.translateToLocal(((Block) obj).getTranslationKey() + ".name");
                } else {
                    str2 = obj.toString();
                }
                Debug.log(String.format("| %-25s| %s", str1, str2));
            }
            Debug.log("|-------------------------------------------------------");
        }

        recipes = null; // Release and have fun GC
        config.save();
    }
    
    @SideOnly(Side.CLIENT)
    @EventHandler
    public void postInit2(FMLPostInitializationEvent event) {
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        ACConfig.updateConfig(null);
    }

    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        config.save();
    }

    @SubscribeEvent
    public void onClientDisconnectionFromServer(
        ClientDisconnectionFromServerEvent e)
    {
        config.save();
    }

    public static void addToRecipe(Class klass) {
        CustomMappingHelper.addMapping(recipes, klass);
    }
    /**
     * Simply a fast route to print debug message.
     */
    public static void debug(Object msg) {
        if (DEBUG_MODE) {
            log.info(msg);
        }
    }

}