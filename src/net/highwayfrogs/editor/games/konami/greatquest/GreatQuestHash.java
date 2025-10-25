package net.highwayfrogs.editor.games.konami.greatquest;

import javafx.scene.control.TextField;
import lombok.Getter;
import net.highwayfrogs.editor.games.generic.data.IGameObject;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash.kcHashedResource;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResource;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.lambda.Consumer5;
import net.highwayfrogs.editor.utils.lambda.TriConsumer;
import net.highwayfrogs.editor.utils.objects.StringNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * This represents a 32-bit hash in Frogger: The Great Quest, and the functionality for working with it.
 * The hashes within this game have mostly been cracked, so the original strings are known.
 * However, there are scenarios where a hash may not correspond to any known string.
 * Strings are superior to use while viewing and modding game data as they are easier to read and modify.
 * However, hash numbers are what the game files store and represent references to other data as.
 * The original developers likely had tools which worked with strings, and exported the hashes from data in their file server.
 * However since we do not have the original data from their server, we have to base our data models on the game files.
 * Thus, this is designed to treat the underlying hash number as the most important piece of data to keep editable,
 * even when the original string is not known.
 * <p/>
 * The original game used unchanging 32-bit integers for hashes, since any changes would output completely new files based data in their file server, we do not have access to those original files.
 * In order to accommodate for changes to hashes (or their underlying string), we must have a pass-by-reference object which can allow hashes to be treated more like object references while also keeping the original value if the resource does not resolve.
 * <p/>
 * Created by Kneesnap on 9/23/2024.
 */
@Getter
public final class GreatQuestHash<TResource extends kcHashedResource> {
    private int hashNumber = -1;
    private String originalString; // The "original" string is the one which created the hash, if known.

    // Some hashes need to use zero for their null value instead of -1, due to how they are processed by the game.
    // For the purposes of distinction, when this is set, we will treat an empty string as zero, and a null string as -1.
    private boolean nullZero;

    // This is the resource associated with the hash number.
    // When this is null, it means the resource was not resolved. (Perhaps we forgot to resolve it, or perhaps it could not be resolved.)
    // It is used as the source of the updated hash, if the hash were to change.
    private TResource resource;

    // When this list contains hashes, this node is called the "master hash", sometimes called the "self hash".
    // This is so that when the master hash updates, all the others (listeners) receive the changes.
    // The listener hashes are able to set their hashes too, and if they do that, they will unlink from the master hash.
    // All references to the master hash are tracked by the master hash object, so when it updates, it can update the others.
    private final List<GreatQuestHash<? extends kcHashedResource>> linkedHashes = new ArrayList<>();

    // Invalidation occurs for the master hash when it is removed, and is no longer valid for all of its linked hashes.
    // The linked hashes will also fire the event, but they will not clear their listeners. Instead, they will lose their resource, but still be valid.
    private final List<Consumer<GreatQuestHash<TResource>>> invalidationListeners = new ArrayList<>();
    private final List<TriConsumer<GreatQuestHash<TResource>, Integer, Integer>> hashChangeListeners = new ArrayList<>();
    private final List<Consumer5<GreatQuestHash<TResource>, String, String, Integer, Integer>> stringChangeListeners = new ArrayList<>();
    private final List<Consumer5<GreatQuestHash<TResource>, TResource, TResource, Integer, String>> resourceChangeListeners = new ArrayList<>();

    public GreatQuestHash() {
        this((TResource) null);
    }

    public GreatQuestHash(int hash) {
        setHash(hash);
    }

    public GreatQuestHash(String originalString) {
        setHash(originalString);
    }

    public GreatQuestHash(int hash, String originalString) {
        setHash(hash, originalString, false);
    }

    public GreatQuestHash(TResource resource) {
        setResource(resource, false);
    }

    /**
     * Returns the hash number as an 8-character hex string.
     * Does not include the original string, if known.
     */
    public String getHashNumberAsString() {
        return NumberUtils.to0PrefixedHexString(this.hashNumber);
    }

    /**
     * Gets the display string, meant for displaying to the user, but NOT modification.
     */
    public String getDisplayString(boolean forceIncludeHash) {
        if (!forceIncludeHash && (this.originalString == null || this.originalString.isEmpty()) && !isHashNullOrEmpty())
            return getHashNumberAsString();

        String dispStr = (this.originalString != null ? "\"" + escape(this.originalString) + "\"" : "null");
        if (forceIncludeHash || this.originalString == null || (this.originalString.isEmpty() && !isHashNullOrEmpty())) {
            return getHashNumberAsString() + "@" + dispStr;
        } else {
            return dispStr;
        }
    }

    /**
     * Gets the string guaranteed to be the inverse of GreatQuest.parseHash(String), making it user-editable.
     */
    public String getAsString() {
        return getAsString(true);
    }

    /**
     * Gets the string guaranteed to be the inverse of GreatQuest.parseHash(String), making it user-editable.
     * NOTE: If escaping is disabled, the caller must handle escaping themselves, otherwise the string may not be a valid inverse!
     */
    public String getAsString(boolean allowEscaping) {
        if (this.originalString != null) {
            if (allowEscaping && shouldBeEscaped(this.originalString)) {
                return "\"" + escape(this.originalString) + "\"";
            } else {
                return this.originalString;
            }
        } else if (this.hashNumber == getValueRepresentingNull()) {
            return "null";
        } else {
            return getHashNumberAsString();
        }
    }

    /**
     * Get as a string usable in a GreatQuest script.
     */
    public String getAsGqsString(kcScriptDisplaySettings settings) {
        if (this.originalString != null && this.originalString.length() > 0) {
            return this.originalString;
        } else if (isHashNull()) {
            return "null";
        } else if (settings != null) {
            return settings.getGqsHashDisplay(this.hashNumber);
        } else {
            return "0x" + getHashNumberAsString();
        }
    }

    /**
     * Set a StringNode to have the hash
     */
    public void applyGqsString(StringNode node, kcScriptDisplaySettings settings) {
        if (this.originalString != null && this.originalString.length() > 0) {
            node.setAsString(this.originalString, true);
        } else {
            kcScriptDisplaySettings.applyGqsSyntaxHashDisplay(node, settings, this.hashNumber, getValueRepresentingNull());
        }
    }

    /**
     * Parses the input string as a hash. The string can contain the hash number in hexadecimal form.
     * This is specified to always be able to parse the output of getAsString()
     * @param input the text to parse the hash from
     */
    public void parse(String input) {
        if (input == null || "null".equals(input)) {
            setHash(null);
        } else if (input.startsWith("\"") && input.endsWith("\"")) {
            setHash(unescape(input.substring(1, input.length() - 1)));
        } else if (input.length() == 8 && NumberUtils.isHexInteger("0x" + input)) {
            setHash(Integer.parseInt(input, 16));
        } else {
            setHash(input);
        }
    }

    private static String escape(String input) {
        if (input == null)
            return null;

        return input.replace("\"", "\\\"")
                .replace("\t", "\\t")
                .replace("\n", "\\n");
    }

    private static boolean shouldBeEscaped(String input) {
        if (input == null)
            return false;

        for (int i = 0; i < input.length(); i++) {
            char temp = input.charAt(i);
            if (temp == '"' || temp == '\n' || temp == '\t' || temp == ' ')
                return true;
        }

        return input.isEmpty();
    }

    private static String unescape(String input) {
        if (input == null)
            return null;

        return input.replace("\\\"", "\"")
                .replace("\\t", "\t")
                .replace("\\n", "\n");
    }

    @Override
    public String toString() {
        return "GreatQuestHash{" + getDisplayString(true) + ",resource=" + this.resource + "}";
    }

    /**
     * Invalidates this hash and all linked hashes/unlinks all listeners.
     * This is done to allow safely deleting an object which still has references in use. (By clearing out those references.)
     */
    public void invalidate() {
        if (!isMaster())
            throw new IllegalStateException("Cannot invalidate " + this + ", as it is not a master hash!");

        onInvalidate();
    }

    private void onInvalidate() {
        fireEvents(this, this.invalidationListeners); // Alert everyone about the invalidation.

        // Unlink all linked hashes.
        while (this.linkedHashes.size() > 0)
            this.linkedHashes.remove(0).onInvalidate();

        // Happens last so all the listeners fire before this finally occurs.
        if (!isMaster())
            setResource(null, false, false); // Events are not fired because the invalidation listener should be used instead.
    }

    /**
     * Update the hash data to match the information provided by the current resource.
     * This should generally only be called for the master hash.
     */
    public void updateHashFromResource() {
        int hashNumber = this.resource != null ? this.resource.calculateHash() : getValueRepresentingNull();
        String originalString = this.resource != null ? this.resource.getResourceName() : null;
        if (this.hashNumber != hashNumber || !Objects.equals(originalString, this.originalString))
            setHash(hashNumber, originalString, false);
    }

    /**
     * Set the original string for the existing hash.
     * If the hash of the provided string does not match the current hash number, an exception will be thrown.
     * @param originalString the string to apply
     */
    public void setOriginalString(String originalString) {
        if (Objects.equals(this.originalString, originalString))
            return; // Already matches.

        int newHash = calculateHash(originalString);
        if (newHash != this.hashNumber)
            throw new IllegalArgumentException("Cannot set '" + originalString + "' to be the original string for " + this + ", as its hash (" + NumberUtils.to0PrefixedHexString(newHash) + ") is incorrect.");

        fireEvents(this, this.stringChangeListeners, this.originalString, originalString, this.hashNumber, newHash);
        this.originalString = originalString;
        for (int i = 0; i < this.linkedHashes.size(); i++)
            this.linkedHashes.get(i).setOriginalString(originalString);
    }

    /**
     * Sets the hash number.
     * Clears the current originalString if the new hash number is new.
     * @param newHash the hash to apply
     */
    public void setHash(int newHash) {
        setHash(newHash, true);
    }

    private void setHash(int newHash, boolean allowResourceReset) {
        if (this.hashNumber == newHash)
            return; // Already matches.

        fireEvents(this, this.hashChangeListeners, this.hashNumber, newHash);
        if (!isMaster() && allowResourceReset)
            setResource(null, false); // Unlink from the current resource.

        // Setup new values.
        this.hashNumber = newHash;
        this.originalString = null;

        // Update linked hashes.
        for (int i = 0; i < this.linkedHashes.size(); i++)
            this.linkedHashes.get(i).setHash(newHash, false);
    }

    /**
     * Sets the hash number and original string to be the freshly supplied string.
     * This should only be used if the hash should be changed, as it will fail if the object is locked.
     * Clears the current originalString if the new hash number is new.
     * @param newOriginalString the string to apply
     */
    public void setHash(String newOriginalString) {
        setHash(newOriginalString, true);
    }

    private void setHash(String newOriginalString, boolean allowResourceReset) {
        int newHash = calculateHash(newOriginalString);
        if (newHash == this.hashNumber) {
            setOriginalString(newOriginalString);
            return;
        }

        // Fire events.
        fireEvents(this, this.hashChangeListeners, this.hashNumber, newHash);
        if (!Objects.equals(this.originalString, newOriginalString))
            fireEvents(this, this.stringChangeListeners, this.originalString, newOriginalString, this.hashNumber, newHash);
        if (!isMaster() && allowResourceReset)
            setResource(null, false); // Unlink from the current resource.

        this.hashNumber = newHash;
        this.originalString = newOriginalString;

        // Update linked hashes.
        for (int i = 0; i < this.linkedHashes.size(); i++)
            this.linkedHashes.get(i).setHash(newOriginalString, false);
    }

    /**
     * Sets the hash number and original string to be the freshly supplied string.
     * Clears the current originalString if the new hash number is new.
     * Unlike the other methods, if requireMatchingHash is false, it is allowed to supply a hash which does not match the string.
     * @param newHash the hash to apply
     * @param newString the string to apply
     * @param requireMatchingHash If true, the string hash will be validated against the provided hash.
     */
    public void setHash(int newHash, String newString, boolean requireMatchingHash) {
        setHash(newHash, newString, requireMatchingHash, true);
    }

    private void setHash(int newHash, String newString, boolean requireMatchingHash, boolean allowResourceReset) {
        if (requireMatchingHash) {
            int realHash = calculateHash(newString);
            if (realHash != newHash)
                throw new IllegalArgumentException("The string provided ('" + newString + "') had a hash of " + NumberUtils.to0PrefixedHexString(realHash) + ", but it did not match the provided hash of " + NumberUtils.to0PrefixedHexString(newHash) + ".");

             if (this.hashNumber == newHash) {
                 setOriginalString(newString);
                 return;
             }
        }

        // Fire events.
        if (this.hashNumber != newHash)
            fireEvents(this, this.hashChangeListeners, this.hashNumber, newHash);
        if (!Objects.equals(this.originalString, newString))
            fireEvents(this, this.stringChangeListeners, this.originalString, newString, this.hashNumber, newHash);

        // Unlink from the current resource as the resource has changed.
        if (!isMaster() && allowResourceReset && this.hashNumber != newHash)
            setResource(null, false);

        // Setup the new stuff.
        this.hashNumber = newHash;
        this.originalString = newString;

        // Apply to linked hashes.
        for (int i = 0; i < this.linkedHashes.size(); i++)
            this.linkedHashes.get(i).setHash(newHash, newString, false, false);
    }


    /**
     * Returns true iff this is the master hash object for its resource.
     */
    public boolean isMaster() {
        return this.resource != null && this.resource.getSelfHash() == this;
    }

    /**
     * Sets the object reference.
     * @param newResource the new reference to apply
     * @param requireCurrentHash Requires that the new calculated hash is the same as the existing one. This should be false unless the hash is resolving its first object.
     */
    public void setResource(TResource newResource, boolean requireCurrentHash) {
        setResource(newResource, requireCurrentHash, true);
    }

    private void setResource(TResource newResource, boolean requireCurrentHash, boolean fireEvents) {
        if (this.resource == newResource)
            return;

        if (isMaster())
            throw new IllegalStateException("Cannot change the resource reference of a master hash object!");

        // Unlink this from the previous resource, it can have its own identity now.
        if (this.resource != null && this.resource.getSelfHash() != null)
            this.resource.getSelfHash().getLinkedHashes().remove(this);

        // Determine the new hash number / string.
        int newHashNumber;
        String newString;
        if (newResource != null) {
            if (newResource.getSelfHash() != null) {
                newHashNumber = newResource.getSelfHash().getHashNumber();
                newString = newResource.getSelfHash().getOriginalString();
            } else {
                newHashNumber = newResource.calculateHash();
                newString = newResource.getResourceName();
            }
        } else {
            newHashNumber = 0;
            newString = null;
        }

        // Validate data before applying.
        if (requireCurrentHash && newHashNumber != this.hashNumber)
            throw new IllegalArgumentException("The new resource's hash is " + NumberUtils.to0PrefixedHexString(newHashNumber) + ", but was required to match the existing hash of " + getHashNumberAsString() + ".");

        // Fire events.
        if (fireEvents) {
            if (this.hashNumber != newHashNumber)
                fireEvents(this, this.hashChangeListeners, this.hashNumber, newHashNumber);
            if (!Objects.equals(this.originalString, newString))
                fireEvents(this, this.stringChangeListeners, this.originalString, newString, this.hashNumber, newHashNumber);
            fireEvents(this, this.resourceChangeListeners, this.resource, newResource, newHashNumber, newString);
        }

        // Apply new data.
        this.resource = newResource;
        this.hashNumber = newHashNumber;
        this.originalString = newString;

        // Link to the new resource's master hash.
        if (newResource != null && newResource.getSelfHash() != null)
            newResource.getSelfHash().getLinkedHashes().add(this);
    }

    private boolean isHashNullOrEmpty() {
        return this.hashNumber == getValueRepresentingNull() || this.hashNumber == 0;
    }

    /**
     * Gets the hash value which represents null for this hash.
     * @return valueRepresentingNull
     */
    public int getValueRepresentingNull() {
        return isNullZero() ? 0 : -1;
    }

    /**
     * Test if the hash represents a null value.
     */
    public boolean isHashNull() {
        return this.hashNumber == getValueRepresentingNull();
    }

    /**
     * Marks -1 as the value for null.
     */
    public GreatQuestHash<TResource> setNullRepresentedAsZero() {
        if (this.nullZero)
            throw new IllegalStateException("Null is already set to be -1.");

        this.nullZero = true;
        if (this.resource == null && this.hashNumber == -1)
            setHash(0, getOriginalString(), false, false);

        return this;
    }

    /**
     * Returns true if a null value should be hashed as -1.
     * Usage of this feature is entirely dependent on how the original game processes the given hash number.
     */
    public boolean isNullZero() {
        return this.nullZero || (this.resource != null && this.resource.getSelfHash() != this && this.resource.getSelfHash().isNullZero());
    }

    /**
     * Calculates the hash of an arbitrary string based on the configuration of this object.
     * @param input the string to hash
     * @return hashValue
     */
    public int calculateHash(String input) {
        if (input != null) {
            return GreatQuestUtils.hashFilePath(input);
        } else {
            return getValueRepresentingNull();
        }
    }

    /**
     * Adds a TextField editor UI to the editor grid for changing the
     * @param grid the grid to setup the TextField for
     * @param chunkedFile the chunked file to resolve assets from
     * @param label the label to show for this hash
     * @param resourceClass the type of resource to resolve new hashes as
     * @return textField
     */
    public TextField addEditorUI(GUIEditorGrid grid, GreatQuestChunkedFile chunkedFile, String label, Class<TResource> resourceClass) {
        if (grid == null)
            throw new NullPointerException("grid");
        if (resourceClass != null && !kcCResource.class.isAssignableFrom(resourceClass))
            throw new IllegalArgumentException("resourceClass " + resourceClass.getSimpleName() + " is not a " + kcCResource.class.getSimpleName() + "!");

        // Master hashes can't be edited, at least not directly here. (Their names are usually set somewhere else and then applied to here)
        if (isMaster()) {
            TextField field = grid.addTextField(label, this.getAsGqsString(null));
            field.setDisable(true);
            return field;
        }

        return grid.addTextField(label, this.getAsGqsString(null), newTargetEntityText -> {
            int hash;
            boolean allowBadHash;
            if (NumberUtils.isHexInteger(newTargetEntityText)) {
                hash = NumberUtils.parseHexInteger(newTargetEntityText);
                allowBadHash = true;
            } else {
                hash = GreatQuestUtils.hash(newTargetEntityText);
                allowBadHash = false;
            }

            kcCResource resource = chunkedFile != null ? chunkedFile.getResourceByHash(hash) : null;
            if (resourceClass != null && resourceClass.isInstance(resource)) {
                this.setResource(resourceClass.cast(resource), false);
                return true;
            } else if (allowBadHash) {
                this.setHash(hash);
                return true;
            } else {
                return false;
            }
        });
    }

    @Override
    public int hashCode() {
        return this.hashNumber;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Integer) {
            return this.hashNumber == (Integer) obj;
        } else if (obj instanceof GreatQuestHash) {
            GreatQuestHash<?> otherHash = (GreatQuestHash<?>) obj;
            return this.hashNumber == otherHash.hashNumber;
        } else {
            return false;
        }
    }

    public interface kcHashedResource extends IGameObject {
        /**
         * Gets the game instance
         */
        GreatQuestInstance getGameInstance();

        /**
         * Gets the name of this resource for hashing purposes.
         * @return hashedName, or null. By default, returning null will cause the hash number to not be updated. In other words, the name will be treated as unknown.
         */
        String getResourceName();

        /**
         * Gets the "master hash" aka the "self hash" for this object.
         * All hashes which reference this object (besides the self hash itself) will be updated when this hash updates.
         * If this is null, there will be no self hash, and other objects will likely be out of sync.
         */
        GreatQuestHash<?> getSelfHash();

        /**
         * Calculates the hash for this object.
         * Usually this is the default hashing behavior, but it can be replaced sometimes for objects which do things differently.
         * @return calculatedHash
         */
        default int calculateHash() {
            return calculateHash(getResourceName());
        }

        /**
         * Gets the hash value of this resource.
         */
        default int getHash() {
            GreatQuestHash<?> hash = getSelfHash();
            return hash != null ? hash.getHashNumber() : 0;
        }

        /**
         * Gets the hash number as a hex string
         */
        default String getHashAsHexString() {
            GreatQuestHash<?> hash = getSelfHash();
            return hash != null ? hash.getHashNumberAsString() : null;
        }

        /**
         * Calculates the hash for an arbitrary input string.
         * Usually this is the default hashing behavior, but it can differ when necessary.
         * @param input the input string to calculate the hash from
         * @return hash
         */
        default int calculateHash(String input) {
            GreatQuestHash<?> selfHash = getSelfHash();
            if (selfHash != null) {
                return selfHash.calculateHash(input);
            } else if (input != null) {
                return GreatQuestUtils.hashFilePath(input);
            } else {
                return 0;
            }
        }
    }

    private static <THash extends GreatQuestHash<?>> void fireEvents(THash hash, List<Consumer<THash>> listeners) {
        if (listeners == null)
            throw new NullPointerException("listeners");

        for (int i = 0; i < listeners.size(); i++) {
            Consumer<THash> listener = listeners.get(i);

            try {
                listener.accept(hash);
            } catch (Throwable th) {
                Utils.handleError(null, th, false, "A listener for %s threw an Exception while running.", hash);
            }

            // If the listener has been removed, reduce the index, so we'll handle the next one.
            if (listeners.size() <= i || listeners.get(i) != listener)
                i--;
        }
    }

    private static <THash extends GreatQuestHash<?>, A, B> void fireEvents(THash hash, List<TriConsumer<THash, A, B>> listeners, A a, B b) {
        if (listeners == null)
            throw new NullPointerException("listeners");

        for (int i = 0; i < listeners.size(); i++) {
            TriConsumer<THash, A, B> listener = listeners.get(i);

            try {
                listener.accept(hash, a, b);
            } catch (Throwable th) {
                Utils.handleError(null, th, false, "A listener for %s threw an Exception while running.", hash);
            }

            // If the listener has been removed, reduce the index, so we'll handle the next one.
            if (listeners.size() <= i || listeners.get(i) != listener)
                i--;
        }
    }

    private static <THash extends GreatQuestHash<?>, A, B, C, D> void fireEvents(THash hash, List<Consumer5<THash, A, B, C, D>> listeners, A a, B b, C c, D d) {
        if (listeners == null)
            throw new NullPointerException("listeners");

        for (int i = 0; i < listeners.size(); i++) {
            Consumer5<THash, A, B, C, D> listener = listeners.get(i);

            try {
                listener.accept(hash, a, b, c, d);
            } catch (Throwable th) {
                Utils.handleError(null, th, false, "A listener for %s threw an Exception while running.", hash);
            }

            // If the listener has been removed, reduce the index, so we'll handle the next one.
            if (listeners.size() <= i || listeners.get(i) != listener)
                i--;
        }
    }
}
