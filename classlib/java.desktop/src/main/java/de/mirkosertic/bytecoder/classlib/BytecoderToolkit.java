/*
 * Copyright 2019 Mirko Sertic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mirkosertic.bytecoder.classlib;

import sun.awt.LightweightFrame;
import sun.awt.SunToolkit;
import sun.awt.datatransfer.DataTransferer;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.dnd.peer.DragSourceContextPeer;
import java.awt.font.TextAttribute;
import java.awt.im.InputMethodHighlight;
import java.awt.im.spi.InputMethodDescriptor;
import java.awt.image.ColorModel;
import java.awt.peer.ButtonPeer;
import java.awt.peer.CheckboxMenuItemPeer;
import java.awt.peer.CheckboxPeer;
import java.awt.peer.ChoicePeer;
import java.awt.peer.DesktopPeer;
import java.awt.peer.DialogPeer;
import java.awt.peer.FileDialogPeer;
import java.awt.peer.FontPeer;
import java.awt.peer.FramePeer;
import java.awt.peer.KeyboardFocusManagerPeer;
import java.awt.peer.LabelPeer;
import java.awt.peer.ListPeer;
import java.awt.peer.MenuBarPeer;
import java.awt.peer.MenuItemPeer;
import java.awt.peer.MenuPeer;
import java.awt.peer.PopupMenuPeer;
import java.awt.peer.RobotPeer;
import java.awt.peer.ScrollPanePeer;
import java.awt.peer.ScrollbarPeer;
import java.awt.peer.SystemTrayPeer;
import java.awt.peer.TextAreaPeer;
import java.awt.peer.TextFieldPeer;
import java.awt.peer.TrayIconPeer;
import java.awt.peer.WindowPeer;
import java.util.Map;
import java.util.Properties;

public class BytecoderToolkit extends SunToolkit {

    @Override
    public WindowPeer createWindow(final Window window) throws HeadlessException {
        return null;
    }

    @Override
    public FramePeer createFrame(final Frame frame) throws HeadlessException {
        return null;
    }

    @Override
    public FramePeer createLightweightFrame(final LightweightFrame lightweightFrame) throws HeadlessException {
        return null;
    }

    @Override
    public DialogPeer createDialog(final Dialog dialog) throws HeadlessException {
        return null;
    }

    @Override
    public ButtonPeer createButton(final Button button) throws HeadlessException {
        return null;
    }

    @Override
    public TextFieldPeer createTextField(final TextField textField) throws HeadlessException {
        return null;
    }

    @Override
    public ChoicePeer createChoice(final Choice choice) throws HeadlessException {
        return null;
    }

    @Override
    public LabelPeer createLabel(final Label label) throws HeadlessException {
        return null;
    }

    @Override
    public ListPeer createList(final List list) throws HeadlessException {
        return null;
    }

    @Override
    public CheckboxPeer createCheckbox(final Checkbox checkbox) throws HeadlessException {
        return null;
    }

    @Override
    public ScrollbarPeer createScrollbar(final Scrollbar scrollbar) throws HeadlessException {
        return null;
    }

    @Override
    public ScrollPanePeer createScrollPane(final ScrollPane scrollPane) throws HeadlessException {
        return null;
    }

    @Override
    public TextAreaPeer createTextArea(final TextArea textArea) throws HeadlessException {
        return null;
    }

    @Override
    public FileDialogPeer createFileDialog(final FileDialog fileDialog) throws HeadlessException {
        return null;
    }

    @Override
    public MenuBarPeer createMenuBar(final MenuBar menuBar) throws HeadlessException {
        return null;
    }

    @Override
    public MenuPeer createMenu(final Menu menu) throws HeadlessException {
        return null;
    }

    @Override
    public PopupMenuPeer createPopupMenu(final PopupMenu popupMenu) throws HeadlessException {
        return null;
    }

    @Override
    public MenuItemPeer createMenuItem(final MenuItem menuItem) throws HeadlessException {
        return null;
    }

    @Override
    public CheckboxMenuItemPeer createCheckboxMenuItem(final CheckboxMenuItem checkboxMenuItem) throws HeadlessException {
        return null;
    }

    @Override
    public DragSourceContextPeer createDragSourceContextPeer(final DragGestureEvent dragGestureEvent) throws InvalidDnDOperationException {
        return null;
    }

    @Override
    public TrayIconPeer createTrayIcon(final TrayIcon trayIcon) throws HeadlessException, AWTException {
        return null;
    }

    @Override
    public SystemTrayPeer createSystemTray(final SystemTray systemTray) {
        return null;
    }

    @Override
    public boolean isTraySupported() {
        return false;
    }

    @Override
    public FontPeer getFontPeer(final String s, final int i) {
        return null;
    }

    @Override
    public RobotPeer createRobot(final Robot robot, final GraphicsDevice graphicsDevice) throws AWTException {
        return null;
    }

    @Override
    public KeyboardFocusManagerPeer getKeyboardFocusManagerPeer() throws HeadlessException {
        return null;
    }

    @Override
    protected int getScreenWidth() {
        return 0;
    }

    @Override
    protected int getScreenHeight() {
        return 0;
    }

    @Override
    protected boolean syncNativeQueue(final long l) {
        return false;
    }

    @Override
    public void grab(final Window window) {

    }

    @Override
    public void ungrab(final Window window) {

    }

    @Override
    public boolean isDesktopSupported() {
        return false;
    }

    @Override
    protected DesktopPeer createDesktopPeer(final Desktop target) throws HeadlessException {
        return null;
    }

    @Override
    public int getScreenResolution() throws HeadlessException {
        return 0;
    }

    @Override
    public ColorModel getColorModel() throws HeadlessException {
        return null;
    }

    @Override
    public void sync() {

    }

    @Override
    public PrintJob getPrintJob(final Frame frame, final String jobtitle, final Properties props) {
        return null;
    }

    @Override
    public void beep() {

    }

    @Override
    public Clipboard getSystemClipboard() throws HeadlessException {
        return null;
    }

    @Override
    public Map<TextAttribute, ?> mapInputMethodHighlight(final InputMethodHighlight highlight) throws HeadlessException {
        return null;
    }

    @Override
    public DataTransferer getDataTransferer() {
        return null;
    }

    @Override
    public InputMethodDescriptor getInputMethodAdapterDescriptor() throws AWTException {
        return null;
    }
}
