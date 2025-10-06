package cn.academy.util;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;

public class ResourcepackUtil {

    @SideOnly(Side.CLIENT)
    public static void generateTexturePack()
    {
        try
        {
            File resourcePacks = Minecraft.getMinecraft().getResourcePackRepository().getDirResourcepacks().getCanonicalFile();
            String destinationBase = resourcePacks.getAbsolutePath() + "/Academy32x";
            copyResourceDirectory("data/Academy32x", destinationBase);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SideOnly(Side.CLIENT)
    public static void copyResourceDirectory(String sourceDir, String destinationDir)
    {
        try
        {
            URI uri = ResourcepackUtil.class.getResource("/" + sourceDir).toURI();
            Path sourcePath;

            // Check if resource is inside a JAR
            if (uri.getScheme().equals("jar"))
            {
                // Create filesystem for JAR
                FileSystem fileSystem;
                try {
                    fileSystem = FileSystems.getFileSystem(uri);
                } catch (FileSystemNotFoundException e) {
                    fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
                }
                sourcePath = fileSystem.getPath("/" + sourceDir);
            }
            else
            {
                // Resource is in a regular directory (dev environment)
                sourcePath = Paths.get(uri);
            }

            Path destPath = Paths.get(destinationDir);

            // Walk through all files and copy them
            Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
                {
                    Path targetDir = destPath.resolve(sourcePath.relativize(dir).toString());
                    if (!Files.exists(targetDir))
                    {
                        Files.createDirectories(targetDir);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
                {
                    Path targetFile = destPath.resolve(sourcePath.relativize(file).toString());
                    if (!Files.exists(targetFile))
                    {
                        Files.copy(file, targetFile);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}