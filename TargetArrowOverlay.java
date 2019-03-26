/*
 * Copyright (c) 2018, Seth <http://github.com/sethtroll>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.worldmaptargets;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.api.coords.WorldPoint;
import java.awt.image.BufferedImage;
import net.runelite.client.util.ImageUtil;
import net.runelite.api.Client;


class TargetArrowOverlay extends Overlay
{

  private static BufferedImage ARROW_ICON = ImageUtil.getResourceStreamFromClass(ImageUtil.class, "/net/runelite/client/plugins/worldmaptargets/target_arrow.png");

  private Client client;

	private final MapTargetPlugin plugin;

	@Inject
	private TargetArrowOverlay(Client client, MapTargetPlugin plugin)
	{
		super(plugin);
		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(OverlayPriority.HIGHEST);
    this.client = client;
		this.plugin = plugin;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
    if (plugin.getCurrentDestination() != null) {
      WorldPoint point = client.getLocalPlayer().getWorldLocation();
      WorldPoint destination = plugin.getCurrentDestination().getWorldPoint();
      int dx = destination.getX() - point.getY();
      int dy = destination.getY() - point.getY();
      double angle = Math.atan(((double)dy)/dx);
      BufferedImage rotatedImage = ImageUtil.rotateImage(ARROW_ICON, angle);
      graphics.drawImage(rotatedImage, 100, 100, null);
    }
    /*Widget minimap = client.getWidget(WidgetInfo.MINIMAP_WORLDMAP_ORB);

		if (minimap != null) {
			Widget[] children = minimap.getChildren();
			if (children == null) {
				System.out.println("no children");
			}
			else {
				System.out.println("child count: " + children.length);
			}
		}*/

    return null;
	}
}
