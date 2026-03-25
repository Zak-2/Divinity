package divinity.module.impl.render.esp;

import static org.lwjgl.opengl.GL11.*;

public final class SkeletonUtils {
    public static void drawSkeletonLines(float[][] entPos, float xOff, float yOff, float rotationYawHead, boolean sneaking) {
        // Right leg
        glPushMatrix();
        glTranslatef(-0.125F, yOff, 0.0F);
        if (entPos[3][0] != 0.0F)
            glRotatef(entPos[3][0] * 57.295776F, 1.0F, 0.0F, 0.0F);
        if (entPos[3][1] != 0.0F)
            glRotatef(entPos[3][1] * 57.295776F, 0.0F, 1.0F, 0.0F);
        if (entPos[3][2] != 0.0F)
            glRotatef(entPos[3][2] * 57.295776F, 0.0F, 0.0F, 1.0F);
        glBegin(GL_LINE_STRIP);
        glVertex3i(0, 0, 0);
        glVertex3f(0.0F, -yOff, 0.0F);
        glEnd();
        glPopMatrix();

        // Left leg
        glPushMatrix();
        glTranslatef(0.125F, yOff, 0.0F);
        if (entPos[4][0] != 0.0F)
            glRotatef(entPos[4][0] * 57.295776F, 1.0F, 0.0F, 0.0F);
        if (entPos[4][1] != 0.0F)
            glRotatef(entPos[4][1] * 57.295776F, 0.0F, 1.0F, 0.0F);
        if (entPos[4][2] != 0.0F)
            glRotatef(entPos[4][2] * 57.295776F, 0.0F, 0.0F, 1.0F);
        glBegin(GL_LINE_STRIP);
        glVertex3i(0, 0, 0);
        glVertex3f(0.0F, -yOff, 0.0F);
        glEnd();
        glPopMatrix();

        glTranslatef(0.0F, 0.0F, sneaking ? 0.25F : 0.0F);
        glPushMatrix();
        glTranslatef(0.0F, sneaking ? -0.05F : 0.0F, sneaking ? -0.01725F : 0.0F);

        // Right arm
        glPushMatrix();
        glTranslatef(-0.375F, yOff + 0.55F, 0.0F);
        if (entPos[1][0] != 0.0F)
            glRotatef(entPos[1][0] * 57.295776F, 1.0F, 0.0F, 0.0F);
        if (entPos[1][1] != 0.0F)
            glRotatef(entPos[1][1] * 57.295776F, 0.0F, 1.0F, 0.0F);
        if (entPos[1][2] != 0.0F)
            glRotatef(-entPos[1][2] * 57.295776F, 0.0F, 0.0F, 1.0F);
        glBegin(GL_LINE_STRIP);
        glVertex3i(0, 0, 0);
        glVertex3f(0.0F, -0.5F, 0.0F);
        glEnd();
        glPopMatrix();

        // Left arm
        glPushMatrix();
        glTranslatef(0.375F, yOff + 0.55F, 0.0F);
        if (entPos[2][0] != 0.0F)
            glRotatef(entPos[2][0] * 57.295776F, 1.0F, 0.0F, 0.0F);
        if (entPos[2][1] != 0.0F)
            glRotatef(entPos[2][1] * 57.295776F, 0.0F, 1.0F, 0.0F);
        if (entPos[2][2] != 0.0F)
            glRotatef(-entPos[2][2] * 57.295776F, 0.0F, 0.0F, 1.0F);
        glBegin(GL_LINE_STRIP);
        glVertex3i(0, 0, 0);
        glVertex3f(0.0F, -0.5F, 0.0F);
        glEnd();
        glPopMatrix();

        glRotatef(xOff - rotationYawHead, 0.0F, 1.0F, 0.0F);

        // Head
        glPushMatrix();
        glTranslatef(0.0F, yOff + 0.55F, 0.0F);
        if (entPos[0][0] != 0.0F)
            glRotatef(entPos[0][0] * 57.295776F, 1.0F, 0.0F, 0.0F);
        glBegin(GL_LINE_STRIP);
        glVertex3i(0, 0, 0);
        glVertex3f(0.0F, 0.3F, 0.0F);
        glEnd();
        glPopMatrix();

        glPopMatrix();

        glRotatef(sneaking ? 25.0F : 0.0F, 1.0F, 0.0F, 0.0F);
        glTranslatef(0.0F, sneaking ? -0.16175F : 0.0F, sneaking ? -0.48025F : 0.0F);

        // Pelvis
        glPushMatrix();
        glTranslated(0.0F, yOff, 0.0F);
        glBegin(GL_LINE_STRIP);
        glVertex3f(-0.125F, 0.0F, 0.0F);
        glVertex3f(0.125F, 0.0F, 0.0F);
        glEnd();
        glPopMatrix();

        // Body
        glPushMatrix();
        glTranslatef(0.0F, yOff, 0.0F);
        glBegin(GL_LINE_STRIP);
        glVertex3i(0, 0, 0);
        glVertex3f(0.0F, 0.55F, 0.0F);
        glEnd();
        glPopMatrix();

        // Chest
        glPushMatrix();
        glTranslatef(0.0F, yOff + 0.55F, 0.0F);
        glBegin(GL_LINE_STRIP);
        glVertex3f(-0.375F, 0.0F, 0.0F);
        glVertex3f(0.375F, 0.0F, 0.0F);
        glEnd();
        glPopMatrix();

        glPopMatrix();
    }
}
