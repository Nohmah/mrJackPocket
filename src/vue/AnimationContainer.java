package src.vue;

import java.util.List;

public class AnimationContainer
{
    public List<Integer> spriteList;
    public int currFrame;
    public double timer;
    public double timeUntilNext;

    public int AddDelta(double delta)
    {
        timer += delta;
        if (timer > timeUntilNext)
        {
            timer = 0;
            currFrame++;
            if (currFrame >= spriteList.size()) currFrame = 0;
        }
        return spriteList.get(currFrame);
    }

    public int NextFrame()
    {
        timer = 0;
        currFrame++;
        if (currFrame >= spriteList.size()) currFrame = 0;
        return spriteList.get(currFrame);
    }



    public AnimationContainer Copier()
    {
        AnimationContainer newAni = new AnimationContainer();
        newAni.spriteList = this.spriteList;
        newAni.timeUntilNext = this.timeUntilNext;
        return newAni;
    }



    public AnimationContainer(List<String> spriteNames, double timeUntilNext)
    {
        this.spriteList = Camera.AddSpriteList(spriteNames);
        this.timeUntilNext = timeUntilNext;
    }

    public AnimationContainer()
    {
        this.timer = 0;
        this.currFrame = 0;
    }
}