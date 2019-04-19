package fi.dy.masa.worldprimer.util;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import fi.dy.masa.worldprimer.WorldPrimer;

public class TemplateWorldPrimer extends Template
{
    private List<Template.BlockInfo> blocks = new ArrayList<>();
    private List<Template.EntityInfo> entities = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public TemplateWorldPrimer()
    {
        super();

        try
        {
            this.blocks = (List<Template.BlockInfo>) (ObfuscationReflectionHelper.findField(Template.class, "field_186270_a").get(this)); // blocks
            this.entities = (List<Template.EntityInfo>) (ObfuscationReflectionHelper.findField(Template.class, "field_186271_b").get(this)); // entities
        }
        catch (Exception e)
        {
            WorldPrimer.logger.error("Failed to reflect the Template class fields", e);
        }
    }

    @Override
    public void takeBlocksFromWorld(World worldIn, BlockPos startPos, BlockPos size, boolean takeEntities, Block toIgnore)
    {
        if (size.getX() >= 1 && size.getY() >= 1 && size.getZ() >= 1)
        {
            BlockPos posEnd = startPos.add(size).add(-1, -1, -1);
            List<Template.BlockInfo> list = Lists.<Template.BlockInfo>newArrayList();
            List<Template.BlockInfo> list1 = Lists.<Template.BlockInfo>newArrayList();
            List<Template.BlockInfo> list2 = Lists.<Template.BlockInfo>newArrayList();
            BlockPos posMin = new BlockPos(Math.min(startPos.getX(), posEnd.getX()), Math.min(startPos.getY(), posEnd.getY()), Math.min(startPos.getZ(), posEnd.getZ()));
            BlockPos posMax = new BlockPos(Math.max(startPos.getX(), posEnd.getX()), Math.max(startPos.getY(), posEnd.getY()), Math.max(startPos.getZ(), posEnd.getZ()));

            ObfuscationReflectionHelper.setPrivateValue(Template.class, this, size, "field_186272_c"); // size

            for (BlockPos.MutableBlockPos posMutable : BlockPos.getAllInBoxMutable(posMin, posMax))
            {
                BlockPos posRel = posMutable.subtract(posMin);
                IBlockState state = worldIn.getBlockState(posMutable);

                if (toIgnore == null || toIgnore != state.getBlock())
                {
                    TileEntity te = worldIn.getTileEntity(posMutable);

                    if (te != null)
                    {
                        NBTTagCompound tag = new NBTTagCompound();
                        tag = Schematic.chiselsAndBitsHandler.writeChiselsAndBitsTileToNBT(new NBTTagCompound(), te);
                        tag.removeTag("x");
                        tag.removeTag("y");
                        tag.removeTag("z");

                        list1.add(new Template.BlockInfo(posRel, state, tag));
                    }
                    else if (state.isFullBlock() == false && state.isFullCube() == false)
                    {
                        list2.add(new Template.BlockInfo(posRel, state, null));
                    }
                    else
                    {
                        list.add(new Template.BlockInfo(posRel, state, null));
                    }
                }
            }

            this.blocks.clear();
            this.blocks.addAll(list);
            this.blocks.addAll(list1);
            this.blocks.addAll(list2);

            if (takeEntities)
            {
                this.takeEntitiesFromWorld(worldIn, posMin, posMax.add(1, 1, 1));
            }
            else
            {
                this.entities.clear();
            }
        }
    }

    private void takeEntitiesFromWorld(World worldIn, BlockPos startPos, BlockPos endPos)
    {
        List<Entity> list = worldIn.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(startPos, endPos), new Predicate<Entity>()
        {
            public boolean apply(@Nullable Entity entity)
            {
                return (entity instanceof EntityPlayer) == false;
            }
        });

        this.entities.clear();

        for (Entity entity : list)
        {
            Vec3d entityPos = new Vec3d(entity.posX - startPos.getX(), entity.posY - startPos.getY(), entity.posZ - startPos.getZ());
            NBTTagCompound tag = new NBTTagCompound();
            entity.writeToNBTOptional(tag);

            BlockPos blockPos;

            if (entity instanceof EntityPainting)
            {
                blockPos = ((EntityPainting) entity).getHangingPosition().subtract(startPos);
            }
            else
            {
                blockPos = new BlockPos(entityPos);
            }

            this.entities.add(new Template.EntityInfo(entityPos, blockPos, tag));
        }
    }
}
