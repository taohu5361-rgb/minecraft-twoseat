package local.twoseat;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(TwoSeat.ID)
public final class TwoSeat {
    public static final String ID = "twoseat";
    public static final Logger LOG = LogUtils.getLogger();
    public TwoSeat() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.register(new SplitSession()));
    }
}
