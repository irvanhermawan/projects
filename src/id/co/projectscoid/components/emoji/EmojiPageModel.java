package id.co.projectscoid.components.emoji;

public interface EmojiPageModel {
  int getIconAttr();
  String[] getEmoji();
  boolean hasSpriteMap();
  String getSprite();
  boolean isDynamic();
}
