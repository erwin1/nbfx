package com.gluonhq.netbeans.nbfx.file.actions;

import java.util.ArrayList;
import java.util.List;

import com.gluonhq.netbeans.nbfx.api.actions.ActionIds;
import com.gluonhq.netbeans.nbfx.api.actions.Command;
import com.gluonhq.netbeans.nbfx.api.actions.ContextAction;
import com.gluonhq.netbeans.nbfx.api.actions.FxActionContext;
import com.gluonhq.netbeans.nbfx.api.actions.RunnableCommand;
import com.gluonhq.netbeans.nbfx.api.view.ViewProvider;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;

/**
 * Builds the file-scoped {@link Command}s (Cut / Copy / Paste / Undo / Redo, registered under the
 * {@code file.*} ids) driven by the global {@link FxActionContext}, the system clipboard and the
 * shared {@link FileUndoManager}.
 *
 * <p>The file selection is the set of {@link FileObject}s carried by the context; the system
 * clipboard is not observable, so Paste enablement is re-read whenever the context changes (the
 * selection or the active view), which includes the navigator regaining focus.</p>
 */
public final class FileActions {

    private final Command copyCommand;
    private final Command cutCommand;
    private final Command pasteCommand;
    private final Command undoCommand;
    private final Command redoCommand;

    public FileActions() {
        FileUndoManager undo = FileUndoManager.getDefault();

        copyCommand = new ContextAction(ActionIds.FILE_COPY, message("CTL_FileCopy"), shortcut(KeyCode.C),
                FileActions::canCopyCut, FileActions::copy, FileObject.class, ViewProvider.class);
        cutCommand = new ContextAction(ActionIds.FILE_CUT, message("CTL_FileCut"), shortcut(KeyCode.X),
                FileActions::canCopyCut, FileActions::cut, FileObject.class, ViewProvider.class);
        pasteCommand = new ContextAction(ActionIds.FILE_PASTE, message("CTL_FilePaste"), shortcut(KeyCode.V),
                FileActions::canPaste, FileActions::paste, FileObject.class, ViewProvider.class);
        undoCommand = RunnableCommand.enabledWhen(ActionIds.FILE_UNDO, message("CTL_FileUndo"),
                new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN),
                undo.canUndoProperty(), undo::undo);
        redoCommand = RunnableCommand.enabledWhen(ActionIds.FILE_REDO, message("CTL_FileRedo"),
                new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
                undo.canRedoProperty(), undo::redo);
    }

    public Command copyCommand() {
        return copyCommand;
    }

    public Command cutCommand() {
        return cutCommand;
    }

    public Command pasteCommand() {
        return pasteCommand;
    }

    public Command undoCommand() {
        return undoCommand;
    }

    public Command redoCommand() {
        return redoCommand;
    }

    static boolean canCopyCut(FxActionContext context) {
        return !realFiles(context).isEmpty();
    }

    static boolean canPaste(FxActionContext context) {
        return FileClipboard.hasFiles() && targetFolder(context) != null;
    }

    private static void copy(FxActionContext context) {
        List<FileObject> files = realFiles(context);
        if (!files.isEmpty()) {
            FileClipboardActions.copy(files);
        }
    }

    private static void cut(FxActionContext context) {
        List<FileObject> files = realFiles(context);
        if (!files.isEmpty()) {
            FileClipboardActions.cut(files);
        }
    }

    private static void paste(FxActionContext context) {
        FileObject target = targetFolder(context);
        if (target != null) {
            FileClipboardActions.paste(target);
        }
    }

    /** The current selection restricted to entries backed by a real file on disk. */
    private static List<FileObject> realFiles(FxActionContext context) {
        List<FileObject> files = new ArrayList<>();
        for (FileObject fo : context.lookupAll(FileObject.class)) {
            if (hasRealFile(fo)) {
                files.add(fo);
            }
        }
        return files;
    }

    /** The folder a paste targets: the first selected entry if a folder, else its parent. */
    static FileObject targetFolder(FxActionContext context) {
        for (FileObject fo : context.lookupAll(FileObject.class)) {
            return fo.isFolder() ? fo : fo.getParent();
        }
        return null;
    }

    private static boolean hasRealFile(FileObject fo) {
        return fo != null && FileUtil.toFile(fo) != null;
    }

    private static KeyCodeCombination shortcut(KeyCode code) {
        return new KeyCodeCombination(code, KeyCombination.SHORTCUT_DOWN);
    }

    private static String message(String key) {
        return NbBundle.getMessage(FileActions.class, key);
    }
}
