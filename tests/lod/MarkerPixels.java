import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

/** Inspect native game screenshots for the two deliberately distant colored test towers. */
class MarkerPixels {
    public static void main(String[] args) throws Exception {
        BufferedImage image = ImageIO.read(new File(args[0]));
        int w=image.getWidth(), h=image.getHeight();
        StringBuilder json=new StringBuilder("{");
        var flags=java.util.Arrays.asList(args);
        int seats=flags.contains("single")?1:2;
        for(int seat=0;seat<seats;seat++) {
            int magenta=0,yellow=0,red=0,cyan=0;
            int center=seats==1?w/2:w*(seat*2+1)/4, radius=w/28;
            for(int y=flags.contains("near")?h/5:h/10;y<3*h/10;y++) for(int x=center-radius;x<center+radius;x++) {
                int rgb=image.getRGB(x,y),r=(rgb>>16)&255,g=(rgb>>8)&255,b=rgb&255;
                if(r>75&&b>65&&g<70)magenta++;
                if(r>100&&g>100&&b<60)yellow++;
                if(r>80&&r>g*2&&r>b*2)red++;
                if(r<80&&g>65&&b>65)cyan++;
            }
            if(seat>0)json.append(',');
            json.append('"').append(seat==0?"host":"guest").append("\":{")
                .append("\"magenta\":").append(magenta).append(",\"yellow\":").append(yellow)
                .append(",\"red\":").append(red).append(",\"cyan\":").append(cyan).append('}');
        }
        System.out.println(json.append('}'));
    }
}
