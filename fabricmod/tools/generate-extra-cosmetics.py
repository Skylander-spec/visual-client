"""Procedural, project-owned textures. Run with Python + Pillow; no network needed."""
from pathlib import Path
from PIL import Image, ImageDraw
import math, random

root = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/visualsfabric'
palette = ['#27313a','#f8f6ed','#f7acc8','#ca703c','#f4d378','#bda0ef','#a0e4f0','#a3dabb',
           '#c9526d','#8db7e8','#4c628a','#83b67c','#bbc8d2','#d7ac89','#e8a452','#fff1cf']
atlas=Image.new('RGBA',(256,256)); draw=ImageDraw.Draw(atlas)
for i,color in enumerate(palette):
    x,y=i%4*64,i//4*64
    draw.rectangle((x,y,x+63,y+63),fill=color)
atlas.save(root/'textures/cosmetic_palette.png')

def cape(name,panel):
    image=Image.new('RGBA',(512,256))
    image.paste(panel,(8,8));image.paste(panel,(96,8))
    image.paste(panel.crop((0,0,80,8)),(8,0))
    image.paste(panel.crop((0,120,80,128)),(8,136))
    image.paste(panel.crop((0,0,8,128)),(0,8))
    image.paste(panel.crop((72,0,80,128)),(88,8))
    image.save(root/'capes'/f'{name}.png')

# White Monster Energy fan motif: monochrome claw mark on an icy white cape.
p=Image.new('RGBA',(80,128),'#f5f6f3');d=ImageDraw.Draw(p)
for y in range(128):
    c=245-int(13*math.sin(y/128*math.pi))
    d.line((0,y,79,y),fill=(c,c+1,min(255,c+2)))
for side in [3,76]:
    for y in range(4,124,8): d.line((side,y,side+(3 if side==3 else -3),y+4),fill='#b9c4c4',width=1)
for shift in [0,17,34]:
    pts=[(17+shift,30),(28+shift,35),(25+shift,42),(27+shift,53),(22+shift,61),
         (24+shift,68),(18+shift,87),(18+shift,67),(16+shift,61),(19+shift,50),(16+shift,44)]
    d.polygon(pts,fill='#182320')
d.line((15,91,65,91),fill='#829b88',width=2)
d.text((17,99),'MONSTER',fill='#27332c')
cape('Monster Energy Weiss',p)

for name,background,ink,mode in [
    ('Kirschbluete','#ecdde8','#cc81ad','flower'),('Panda Liebe','#eeeae1','#2f3542','panda'),
    ('Mondkatze','#303b61','#f6e8ac','moon'),('Wolkenzucker','#b8ddeb','#ffffff','cloud'),
    ('Mint Herzen','#c5eadb','#649d95','heart'),('Pfirsich','#ffe0c7','#ed9b8d','heart'),
    ('Lavendel','#bcb1df','#f4eaff','star'),('Sternenreise','#283452','#edddaa','star')]:
    p=Image.new('RGBA',(80,128),background);d=ImageDraw.Draw(p);rng=random.Random(name)
    for n in range(13):
        x,y=rng.randrange(8,69),rng.randrange(9,118)
        if mode=='heart':
            d.ellipse((x-4,y-3,x+1,y+2),fill=ink);d.ellipse((x,y-3,x+5,y+2),fill=ink)
            d.polygon([(x-4,y),(x+5,y),(x,y+6)],fill=ink)
        elif mode=='flower':
            for a in range(5):
                dx,dy=math.cos(a*1.256)*4,math.sin(a*1.256)*4
                d.ellipse((x+dx-2,y+dy-2,x+dx+2,y+dy+2),fill=ink)
            d.ellipse((x-1,y-1,x+1,y+1),fill='#fff2d4')
        elif mode=='cloud':
            d.ellipse((x-6,y,x+6,y+6),fill=ink);d.ellipse((x-3,y-3,x+3,y+4),fill=ink)
        else:
            d.line((x-2,y,x+2,y),fill=ink);d.line((x,y-2,x,y+2),fill=ink)
    if mode=='panda':
        d.ellipse((15,36,31,54),fill=ink);d.ellipse((49,36,65,54),fill=ink)
        d.ellipse((16,42,64,90),fill='#fcfcf8')
        for x in [30,50]:
            d.ellipse((x-7,57,x+7,74),fill=ink);d.ellipse((x-2,61,x+1,65),fill='white')
        d.ellipse((36,73,44,78),fill=ink)
    if mode=='moon':
        d.ellipse((17,32,63,80),fill=ink);d.ellipse((28,25,68,69),fill=background)
        d.polygon([(28,97),(28,80),(32,73),(37,79),(42,73),(48,81),(50,97)],fill=ink)
    cape(name,p)
print('Generated palette and 9 cape textures')
