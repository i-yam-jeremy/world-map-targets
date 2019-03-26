/*
 * Copyright (c) 2019, i-yam-jeremy (https://github.com/i-yam-jeremy)
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

// Based on MenuEntrySwapper Plugin
/*
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * Copyright (c) 2018, Kamiel
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

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.util.Set;
import java.util.Arrays;
import javax.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.events.ConfigChanged;
import net.runelite.api.events.FocusChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PostItemComposition;
import net.runelite.api.events.WidgetHiddenChanged;
import net.runelite.api.events.WidgetMenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.input.KeyManager;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.menus.WidgetMenuOption;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.ArrayUtils;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.api.coords.WorldPoint;
import java.awt.image.BufferedImage;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.ui.overlay.OverlayManager;


@PluginDescriptor(
	name = "Map Targets",
	description = "Set destination targets on the world map to get a direction arrow",
	tags = {"map", "destination", "target", "location", "directions", "travel", "world"}
)
public class MapTargetPlugin extends Plugin implements MouseListener
{

	private static String MENUOP_SET_TARGET = "Set Target";

	private static BufferedImage MARKER_ICON = ImageUtil.getResourceStreamFromClass(ImageUtil.class, "/net/runelite/client/plugins/worldmaptargets/marker_blue.png");

	@Inject
	private Client client;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private WorldMapPointManager worldMapPointManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private TargetArrowOverlay targetArrowOverlay;

	private WorldMapPoint currentDestination;


	@Override
	public void startUp()
	{
		currentDestination = null;
		mouseManager.registerMouseListener(this);
		overlayManager.add(targetArrowOverlay);
	}

	@Override
	public void shutDown()
	{
		currentDestination = null;
		mouseManager.unregisterMouseListener(this);
		overlayManager.remove(targetArrowOverlay);
	}

	@Subscribe
	public void onWidgetHiddenChanged(WidgetHiddenChanged event) {
		/*Widget worldMap = client.getWidget(WidgetInfo.WORLD_MAP_VIEW);
		if (worldMap != null && event.getWidget() != null && worldMap.getId() == event.getWidget().getId()) {
			System.out.println("Found world map!");
		}*/
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event) {
		int widgetID = event.getActionParam1();
		Widget worldMap = client.getWidget(WidgetInfo.WORLD_MAP_OVERVIEW_MAP);
		if (worldMap != null && worldMap.getId() == widgetID) {
			System.out.println("WORLD MAP");
			MenuEntry[] menuEntries = client.getMenuEntries();

			MenuEntry newMenuEntry = createSetTargetOptionMenuEntry(event);
			menuEntries = Arrays.copyOf(menuEntries, menuEntries.length + 1);
			menuEntries[menuEntries.length - 1] = newMenuEntry;

			client.setMenuEntries(menuEntries);
		}
	}

	private MenuEntry createSetTargetOptionMenuEntry(MenuEntryAdded event) {
		int widgetIndex = event.getActionParam0();
		int widgetID = event.getActionParam1();

		MenuEntry menuEntry = new MenuEntry();
		menuEntry.setTarget(event.getTarget());
		menuEntry.setOption(MENUOP_SET_TARGET);
		menuEntry.setParam0(widgetIndex);
		menuEntry.setParam1(widgetID);
		menuEntry.setType(MenuAction.RUNELITE.getId());

		return menuEntry;
}

	@Subscribe
	public void onWidgetMenuOptionClicked(WidgetMenuOptionClicked event)
	{
		//TODO
		/*if (event.getWidget() == WidgetInfo.FIXED_VIEWPORT_INVENTORY_TAB
			|| event.getWidget() == WidgetInfo.RESIZABLE_VIEWPORT_INVENTORY_TAB
			|| event.getWidget() == WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_TAB)
		{
			configuringShiftClick = event.getMenuOption().equals(CONFIGURE) && Text.removeTags(event.getMenuTarget()).equals(MENU_TARGET);
			refreshShiftClickCustomizationMenus();
		}*/
	}

	public WorldMapPoint getCurrentDestination() {
		return currentDestination;
	}

	public MouseEvent mouseClicked(MouseEvent mouseEvent) {
		return mouseEvent;
	}

	public MouseEvent mousePressed(MouseEvent mouseEvent) {
		if (mouseEvent.isShiftDown()) {
			Widget worldMap = client.getWidget(WidgetInfo.WORLD_MAP_VIEW);

			if (worldMap != null) {
				try {
					java.lang.reflect.Method method = worldMapPointManager.getClass().getDeclaredMethod("getWorldMapPoints", new Class<?>[]{});
					method.setAccessible(true);
					java.util.List<WorldMapPoint> worldMapPoints = (java.util.List<WorldMapPoint>) method.invoke(worldMapPointManager, new Object[]{});

					WorldMapPoint p1 = null;
					WorldMapPoint p2 = null;
					for (WorldMapPoint worldMapPoint : worldMapPoints) {
						if (worldMapPoint.getClickbox() != null) {
							if (p1 == null) {
								p1 = worldMapPoint;
							}
							else {
								p2 = worldMapPoint;
								break;
							}
						}
					}

					double scale = (p1.getClickbox().getX() - p2.getClickbox().getX()) / (p1.getWorldPoint().getX() - p2.getWorldPoint().getX());

					for (WorldMapPoint worldMapPoint : worldMapPoints) {
						if (worldMapPoint.getClickbox() != null && worldMapPoint.getClickbox().getX() > 0 && worldMapPoint.getClickbox().getY() > 0) {
							int mouseX = mouseEvent.getX();
							int mouseY = mouseEvent.getY();
							int mapPointScreenX = (int) worldMapPoint.getClickbox().getX();
							int mapPointScreenY = (int) worldMapPoint.getClickbox().getY();
							int screenDx = mouseX - mapPointScreenX;
							int screenDy = mouseY - mapPointScreenY;
							int worldDx = (int) (screenDx / scale);
							int worldDy = (int) (-screenDy / scale);
							WorldPoint destination = worldMapPoint.getWorldPoint().dx(worldDx).dy(worldDy);
							System.out.println("(" + destination.getX() + ", " + destination.getY() + ")");

							if (currentDestination != null) {
								worldMapPointManager.remove(currentDestination);
							}
							currentDestination = new WorldMapPoint(destination, MARKER_ICON);
							worldMapPointManager.add(currentDestination);
							break;
						}
						/*System.out.println(worldMapPoint.getClickbox());
						System.out.println(worldMapPoint.getWorldPoint().getX() + ", " + worldMapPoint.getWorldPoint().getY() + ", " + worldMapPoint.getWorldPoint().getPlane());*/
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				/*System.out.println("WORLD MAP BOUNDS: " + worldMap.getBounds());
				System.out.println("(" + worldMap.getRelativeX() + ", " + worldMap.getRelativeY() + ")");
				//System.out.println("scroll: (" + worldMap.getScrollX() + ", " + worldMap.getScrollY() + ")");*/
			}
		}

		return mouseEvent;
	}

	public MouseEvent mouseReleased(MouseEvent mouseEvent) {
		return mouseEvent;
	}

	public MouseEvent mouseEntered(MouseEvent mouseEvent) {
		return mouseEvent;
	}

	public MouseEvent mouseExited(MouseEvent mouseEvent) {
		return mouseEvent;
	}

	public MouseEvent mouseDragged(MouseEvent mouseEvent) {
		return mouseEvent;
	}

	public MouseEvent mouseMoved(MouseEvent mouseEvent) {
		return mouseEvent;
	}

}
