/****************************************************************************
 Copyright (c) 2013      Zynga Inc.
 Copyright (c) 2013-2016 Chukong Technologies Inc.

 http://www.cocos2d-x.org

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 ****************************************************************************/

#ifndef _COCOS2D_CCLABEL_H_
#define _COCOS2D_CCLABEL_H_

#include "2d/CCNode.h"
#include "2d/CCFontAtlas.h"
#include "base/CCProtocols.h"

NS_CC_BEGIN

/**
 * @addtogroup _2d
 * @{
 */


/**
 * @struct TTFConfig
 * @see `GlyphCollection`
 */
typedef struct _ttfConfig
{
    std::string fontFilePath;
    const char* customGlyphs;
    float fontSize;
    int outlineSize;

    GlyphCollection glyphs;
    bool distanceFieldEnabled;
    bool italics;
    bool bold;
    bool underline;
    bool strikethrough;

    _ttfConfig(const std::string& filePath = "",float size = 12, const GlyphCollection& glyphCollection = GlyphCollection::DYNAMIC,
        const char *customGlyphCollection = nullptr, bool useDistanceField = false, int outline = 0,
               bool useItalics = false, bool useBold = false, bool useUnderline = false, bool useStrikethrough = false)
        : fontFilePath(filePath)
        , customGlyphs(customGlyphCollection)
        , fontSize(size)
        , outlineSize(outline)
        , glyphs(glyphCollection)
        , distanceFieldEnabled(useDistanceField)
        , italics(useItalics)
        , bold(useBold)
        , underline(useUnderline)
        , strikethrough(useStrikethrough)
    {
        if(outline > 0)
        {
            distanceFieldEnabled = false;
        }
    }
} TTFConfig;

enum class TextFormatter : char
{
    NewLine = '\n',
    CarriageReturn = '\r',
    NextCharNoChangeX = '\b'
};

class Sprite;
class SpriteBatchNode;
class DrawNode;
class EventListenerCustom;
class SpriteFrame;
class SpriteProgram;
/**
 * @brief Label is a subclass of Node that knows how to render text labels.
 *
 * Label can be created with:
 * - A true type font file.
 * - A bitmap font file.
 * - A char map file.
 * - The built in system font.
 *
 * Bitmap Font supported editors:
 * - http://glyphdesigner.71squared.com/ (Commercial, Mac OS X)
 * - http://www.n4te.com/hiero/hiero.jnlp (Free, Java)
 * - http://slick.cokeandcode.com/demos/hiero.jnlp (Free, Java)
 * - http://www.angelcode.com/products/bmfont/ (Free, Windows only)
 * @js NA
 */
class CC_DLL Label : public Node, public LabelProtocol, public BlendProtocol
{
public:
    enum class Overflow : char
    {
        //In NONE mode, the dimensions is (0,0) and the content size will change dynamically to fit the label.
        NONE,
        /**
         *In CLAMP mode, when label content goes out of the bounding box, it will be clipped.
         */
        CLAMP,
        /**
         * In SHRINK mode, the font size will change dynamically to adapt the content size.
         */
        SHRINK,
        /**
         *In RESIZE_HEIGHT mode, you can only change the width of label and the height is changed automatically.
         */
        RESIZE_HEIGHT
    };
    /// @name Creators
    /// @{

    /**
    * Allocates and initializes a Label, with default settings.
    *
    * @return An automatically released Label object.
    */
    static Label* create();

    /**
     * Allocates and initializes a Label, base on platform-dependent API.
     *
     * @param text The initial text.
     * @param font A font file or a font family name.
     * @param fontSize The font size. This value must be > 0.
     * @param dimensions
     * @param hAlignment The text horizontal alignment.
     * @param vAlignment The text vertical alignment.
     *
     * @warning It will generate texture by the platform-dependent code.
     *
     * @return An automatically released Label object.
     */
    static Label* createWithSystemFont(const std::string& text,
                                       const std::string& font,
                                       float fontSize,
                                       const Size& dimensions = Size::ZERO,
                                       TextHAlignment hAlignment = TextHAlignment::LEFT,
                                       TextVAlignment vAlignment = TextVAlignment::TOP);

    /**
    * Allocates and initializes a Label, base on FreeType2.
    *
    * @param text The initial text.
    * @param fontFilePath A font file.
    * @param fontSize The font size. This value must be > 0.
    * @param dimensions
    * @param hAlignment The text horizontal alignment.
    * @param vAlignment The text vertical alignment.
    *
    * @return An automatically released Label object.
    */
    static Label * createWithTTF(const std::string& text,
                                 const std::string& fontFilePath,
                                 float fontSize,
                                 const Size& dimensions = Size::ZERO,
                                 TextHAlignment hAlignment = TextHAlignment::LEFT,
                                 TextVAlignment vAlignment = TextVAlignment::TOP);

    /**
    * Allocates and initializes a Label, base on FreeType2.
    *
    * @param ttfConfig A pointer to a TTFConfig object.
    * @param text The initial text.
    * @param hAlignment The text horizontal alignment.
    * @param maxLineWidth The max line width.
    *
    * @return An automatically released Label object.
    * @see TTFConfig setTTFConfig setMaxLineWidth
    */
    static Label* createWithTTF(const TTFConfig& ttfConfig,
                                const std::string& text,
                                TextHAlignment hAlignment = TextHAlignment::LEFT,
                                int maxLineWidth = 0);

    /**
    * Allocates and initializes a Label, with a bitmap font file.
    *
    * @param bmfontString A bitmap font string content
    * @param text The initial text.
    * @param hAlignment Text horizontal alignment.
    * @param maxLineWidth The max line width.
    * @param imageOffset
    *
    * @return An automatically released Label object.
    * @see setBMFontFilePath setMaxLineWidth
    */
    static Label* createWithBMFont(const std::string& bmfontString,
                                   const std::string& text,
                                   SpriteFrame* spriteFrame,
                                   const TextHAlignment& hAlignment = TextHAlignment::LEFT,
                                   int maxLineWidth = 0,
                                   const Vec2& imageOffset = Vec2::ZERO);
    

    /**
    * Allocates and initializes a Label, with char map configuration.
    *
    * @param charMapFile A char map file, it's a PNG format.
    * @param itemWidth The width in points of each element.
    * @param itemHeight The height in points of each element.
    * @param startCharMap The starting char of the char map.
    *
    * @return An automatically released Label object.
    */
    static Label * createWithCharMap(const std::string& charMapFile,
                                     int itemWidth,
                                     int itemHeight,
                                     int startCharMap);

    /**
    * Allocates and initializes a Label, with char map configuration.
    *
    * @param texture A pointer to an existing Texture2D object.
    * @param itemWidth The width in points of each element.
    * @param itemHeight The height in points of each element.
    * @param startCharMap The starting char of the char map.
    *
    * @return An automatically released Label object.
    */
    static Label * createWithCharMap(Texture2D* texture,
                                     int itemWidth,
                                     int itemHeight,
                                     int startCharMap);

    /**
    * Allocates and initializes a Label, with char map configuration.
    *
    * @param plistFile A configuration file of char map.
    *
    * @return An automatically released Label object.
    */
    static Label * createWithCharMap(const std::string& plistFile);

    //  end of creators group
    /// @}

    /// @{
    /// @name Font methods

    /**
     * Sets a new TTF configuration to Label.
     * @see `TTFConfig`
     */
    virtual bool setTTFConfig(const TTFConfig& ttfConfig);

    /**
     * Returns the TTF configuration object used by the Label.
     * @see `TTFConfig`
     */
    virtual const TTFConfig& getTTFConfig() const { return _fontConfig;}

    /** Sets a new bitmap font to Label */
    virtual bool setBMFontFilePath(const std::string& bmfontDataString,
                                   SpriteFrame* spriteFrame,
                                   const Vec2& imageOffset = Vec2::ZERO,
                                   float fontSize = 0);

    /** Returns the bitmap font used by the Label.*/
    const std::string& getBMFontFilePath() const { return _bmFontPath;}

    /**
     * Sets a new char map configuration to Label.
     *
     * @see `createWithCharMap(const std::string&,int,int,int)`
     */
    virtual bool setCharMap(const std::string& charMapFile, int itemWidth, int itemHeight, int startCharMap);

    /**
     * Sets a new char map configuration to Label.
     *
     * @see `createWithCharMap(Texture2D*,int,int,int)`
     */
    virtual bool setCharMap(Texture2D* texture, int itemWidth, int itemHeight, int startCharMap);

    /**
     * Sets a new char map configuration to Label.
     *
     * @see `createWithCharMap(const std::string&)`
     */
    virtual bool setCharMap(const std::string& plistFile);

    /**
     * Sets a new system font to Label.
     *
     * @param font A font file or a font family name.
     * @warning
     */
    virtual void setSystemFontName(const std::string& font);

    /** Returns the system font used by the Label.*/
    virtual const std::string& getSystemFontName() const { return _systemFont;}

    /* Sets the system font size of Label.*/
    virtual void setSystemFontSize(float fontSize);

    /** Returns the bitmap font path used by the Label.*/
    virtual float getSystemFontSize() const { return _systemFontSize;}

    /**
     * @warning This method is not recommended for game developers.
     */
    virtual void requestSystemFontRefresh() { _systemFontDirty = true;}

    //  end of font methods
    /// @}

    /** Sets the text that this Label is to display.*/
    virtual void setString(const std::string& text) override;


    /** Return the text the Label is currently displaying.*/
    virtual const std::string& getString() const override {  return _utf8Text; }

    /**
     * Return the number of lines of text.
     */
    int getStringNumLines();

    /**
     * Return length of string.
     */
    int getStringLength();

    /**
     * Sets the text color of Label.
     *
     * The text color is different from the color of Node.
     *
     * @warning Limiting use to only when the Label created with true type font or system font.
     */
    virtual void setTextColor(const Color4B &color);

    /** Returns the text color of the Label.*/
    const Color4B& getTextColor() const { return _textColor;}

    /**
     * Enable shadow effect to Label.
     *
     * @todo Support blur for shadow effect.
     */
    virtual void enableShadow(const Color4B& shadowColor = Color4B::BLACK,const Size &offset = Size(2,-2), int blurRadius = 0);

    /**
     * Enable outline effect to Label.
     * @warning Limiting use to only when the Label created with true type font or system font.
     */
    virtual void enableOutline(const Color4B& outlineColor,int outlineSize = -1);

    /**
    * Enable glow effect to Label.
    * @warning Limiting use to only when the Label created with true type font.
    */
    virtual void enableGlow(const Color4B& glowColor);

	virtual void enableGradient(const Color4B& startColor = Color4B::WHITE, const Color4B &endColod = Color4B::WHITE, float angle = 0.0f);
    /**
     * Enable italics rendering
     */
    void enableItalics();

    /**
     * Enable bold rendering
     */
    void enableBold();

    /**
     * Enable underline
     */
    void enableUnderline();

    /**
     * Enables strikethrough.
     * Underline and Strikethrough cannot be enabled at the same time.
     * Strikethrough is like an underline but at the middle of the glyph
     */
    void enableStrikethrough();
    /**
     * Disable all effect applied to Label.
     * @warning Please use disableEffect(LabelEffect::ALL) instead of this API.
     */
    virtual void disableEffect();

    /**
     * Disable effect to Label.
     *
     * @see `LabelEffect`
     */
    virtual void disableEffect(LabelEffect effect);

    /**
    * Return whether the shadow effect is enabled.
    */
    bool isShadowEnabled() const { return _currLabelEffect.isOn(LabelEffect::SHADOW); }

    /**
    * Return shadow effect offset value.
    */
    Size getShadowOffset() const { return _shadowOffset; }

    /**
    * Return the shadow effect blur radius.
    */
    float getShadowBlurRadius() const { return _shadowBlurRadius; }

    /**
    * Return the shadow effect color value.
    */
    Color4F getShadowColor() const { return _shadowColor4F; }

    /**
    * Return the outline effect size value.
    */
    int getOutlineSize() const { return _outlineSize; }

    /**
    * Return current effect type.
    */
    Flag getLabelEffectType() const { return _currLabelEffect; }

    /**
    * Return current effect color value.
    */
    Color4F getEffectColor() const { return _effectColorF; }

	void setGradientColorStart(const Color4B& color);

	void setGradientColorEnd(const Color4B& color);

	void setGradientAngle(float angle) { _gradientAngle = CC_DEGREES_TO_RADIANS(angle); }

    /** Sets the Label's text horizontal alignment.*/
    void setAlignment(TextHAlignment hAlignment) { setAlignment(hAlignment,_vAlignment);}

    /** Returns the Label's text horizontal alignment.*/
    TextHAlignment getTextAlignment() const { return _hAlignment;}

    /** Sets the Label's text alignment.*/
    void setAlignment(TextHAlignment hAlignment,TextVAlignment vAlignment);

    /** Sets the Label's text horizontal alignment.*/
    void setHorizontalAlignment(TextHAlignment hAlignment) { setAlignment(hAlignment,_vAlignment); }

    /** Returns the Label's text horizontal alignment.*/
    TextHAlignment getHorizontalAlignment() const { return _hAlignment; }

    /** Sets the Label's text vertical alignment.*/
    void setVerticalAlignment(TextVAlignment vAlignment) { setAlignment(_hAlignment,vAlignment); }

    /** Returns the Label's text vertical alignment.*/
    TextVAlignment getVerticalAlignment() const { return _vAlignment; }

    /**
     * Specify what happens when a line is too long for Label.
     *
     * @param breakWithoutSpace Lines are automatically broken between words if this value is false.
     */
    void setLineBreakWithoutSpace(bool breakWithoutSpace);

    /**
     * Makes the Label at most this line untransformed width.
     * The Label's max line width be used for force line breaks if the value not equal zero.
     */
    void setMaxLineWidth(float maxLineWidth);
    float getMaxLineWidth() { return _maxLineWidth; }
    /**
     * Change font size of label type BMFONT
     * Note: This function only scale the BMFONT letter to mimic the font size change effect.
     *
     * @param fontSize The desired font size in float.
     */
    void setBMFontSize(float fontSize);

    /**
     * Return the user define BMFont size.
     *
     * @return The BMFont size in float value.
     */
    float getBMFontSize()const;

    /**
     * Toggle wrap option of the label.
     * Note: System font doesn't support manually toggle wrap.
     *
     * @param enable Set true to enable wrap and false to disable wrap.
     */
    void enableWrap(bool enable);

    /**
     * Query the wrap is enabled or not.
     * Note: System font will always return true.
     */
    bool isWrapEnabled()const;

    /**
     * Change the label's Overflow type, currently only TTF and BMFont support all the valid Overflow type.
     * Char Map font supports all the Overflow type except for SHRINK, because we can't measure it's font size.
     * System font only support Overflow::Normal and Overflow::RESIZE_HEIGHT.
     *
     * @param overflow   see `Overflow`
     */
    void setOverflow(Overflow overflow);

    /**
     * Query the label's Overflow type.
     *
     * @return see `Overflow`
     */
    Overflow getOverflow()const;
    /**
     * Makes the Label exactly this untransformed width.
     *
     * The Label's width be used for text align if the value not equal zero.
     */
    void setWidth(float width) { setDimensions(width,_labelHeight);}
    float getWidth() const { return _labelWidth; }

    /**
     * Makes the Label exactly this untransformed height.
     *
     * The Label's height be used for text align if the value not equal zero.
     * The text will display incomplete if the size of Label is not large enough to display all text.
     */
    void setHeight(float height){ setDimensions(_labelWidth, height); }
    float getHeight() const { return _labelHeight; }

    /** Sets the untransformed size of the Label in a more efficient way. */
    void setDimensions(float width, float height);
    const Size& getDimensions() const{ return _labelDimensions;}

    /** Update content immediately.*/
    virtual void updateContent();

    /**
     * Provides a way to treat each character like a Sprite.
     * @warning No support system font.
     */
    virtual Sprite * getLetter(int lettetIndex);

    /** Clips upper and lower margin to reduce height of Label.*/
    void setClipMarginEnabled(bool clipEnabled) { _clipEnabled = clipEnabled; }

    bool isClipMarginEnabled() const { return _clipEnabled; }

    /** Sets the line height of the Label.
     * @warning Not support system font.
     * @since v3.2.0
     */
    void setLineHeight(float height);

    /**
     * Returns the line height of this Label.
     * @warning Not support system font.
     * @since v3.2.0
     */
    float getLineHeight() const;

    void setLineSpacing(float height);
    float getLineSpacing() const;

    /**
     * Sets the additional kerning of the Label.
     *
     * @warning Not support system font.
     * @since v3.2.0
     */
    void setAdditionalKerning(float space);

    /**
     * Returns the additional kerning of the Label.
     *
     * @warning Not support system font.
     * @since v3.2.0
     */
    float getAdditionalKerning() const;

    FontAtlas* getFontAtlas() { return _fontAtlas; }

    virtual const BlendFunc& getBlendFunc() const override { return _blendFunc; }
    virtual void setBlendFunc(const BlendFunc &blendFunc) override;

    virtual bool isOpacityModifyRGB() const override { return _isOpacityModifyRGB; }
    virtual void setOpacityModifyRGB(bool isOpacityModifyRGB) override;
    virtual void updateDisplayedColor(const Color3B& parentColor) override;
    virtual void updateDisplayedOpacity(GLubyte parentOpacity) override;

    virtual std::string getDescription() const override;

    virtual const Size& getContentSize() const override;
    virtual Rect getBoundingBox() const override;

    virtual void visit(IRenderer *renderer, const Mat4 &parentTransform, uint32_t parentFlags) override;
    virtual void draw(IRenderer *renderer, const Mat4 &transform, uint32_t flags) override;

    virtual void removeAllChildrenWithCleanup(bool cleanup) override;
    virtual void removeChild(Node* child, bool cleanup = true) override;
    virtual void setGlobalZOrder(float globalZOrder) override;

    virtual void setFontDefinition(const FontDefinition& textDefinition);
    FontDefinition getFontDefinition() const { return _getFontDefinition(); }

CC_CONSTRUCTOR_ACCESS:
    /**
     * Constructor of Label.
     * @js NA
     */
    Label();

    /**
     * Destructor of Label.
     * @js NA
     * @lua NA
     */
    virtual ~Label();

    bool initWithTTF(const std::string& text, const std::string& fontFilePath, float fontSize,
                     const Size& dimensions = Size::ZERO, TextHAlignment hAlignment = TextHAlignment::LEFT,
                     TextVAlignment vAlignment = TextVAlignment::TOP);

    bool initWithTTF(const TTFConfig& ttfConfig, const std::string& text,
                     TextHAlignment hAlignment = TextHAlignment::LEFT, int maxLineWidth = 0);

protected:
    struct LetterInfo
    {
        char16_t utf16Char;
        bool valid;
        float positionX;
        float positionY;
        int atlasIndex;
        int lineIndex;
        int colorIndex;
    };

    enum class LabelType : char {
        TTF,
        BMFONT,
        CHARMAP,
        STRING_TEXTURE
    };

    virtual void setFontAtlas(FontAtlas* atlas, bool distanceFieldEnabled = false, bool useA8Shader = false);

    void computeStringNumLines();

    void onDraw(const Mat4& transform, bool transformUpdated);
    void onDrawShadow(GLProgram* glProgram, const Color4F& shadowColor);
    void drawSelf(IRenderer* renderer, uint32_t flags);

    bool multilineTextWrapByChar();
    bool multilineTextWrapByWord();
    bool multilineTextWrap(const std::function<int(const std::u16string&, int, int)>& lambda);
    void shrinkLabelToContentSize(const std::function<bool(void)>& lambda);
    bool isHorizontalClamp();
    bool isVerticalClamp();
    float getRenderingFontSize()const;
    void rescaleWithOriginalFontSize();

    void updateLabelLetters();
    virtual bool alignText();
    void computeAlignmentOffset();
    bool computeHorizontalKernings(const std::u16string& stringToRender);
	bool colorText(const std::u16string& utf16string);

    void recordLetterInfo(const cocos2d::Vec2& point, char16_t utf16Char, int letterIndex, int lineIndex);
    void recordPlaceholderInfo(int letterIndex, char16_t utf16Char);

    bool updateQuads();

    void createSpriteForSystemFont(const FontDefinition& fontDef);
    void createShadowSpriteForSystemFont(const FontDefinition& fontDef);

    virtual void updateShaderProgram();
    void updateBMFontScale();
    void scaleFontSizeDown(float fontSize);
    bool setTTFConfigInternal(const TTFConfig& ttfConfig);
    void setBMFontSizeInternal(float fontSize);
    bool isHorizontalClamped(float letterPositionX, int lineInex);
    void restoreFontSize();
    void updateLetterSpriteScale(Sprite* sprite);
    int getFirstCharLen(const std::u16string& utf16Text, int startIndex, int textLen);
    int getFirstWordLen(const std::u16string& utf16Text, int startIndex, int textLen);

    void reset();

    FontDefinition _getFontDefinition() const;

    virtual void updateColor() override;

    Vector<SpriteBatchNode*> _batchNodes;
    std::vector<LetterInfo> _lettersInfo;
    std::vector<float> _linesWidth;
    std::vector<float> _linesOffsetX;

	struct stringNumIndex
	{
		int start;
		int end;
	};

    std::vector<Color3B> _colorSection;
    std::vector<stringNumIndex> _colorIndexNum;


    std::unordered_map<int, Sprite*> _letters;

    std::u16string _utf16Text;
    std::string _utf8Text;
    std::string _bmFontPath;
    std::string _systemFont;

    SmartPtr<Sprite> _textSprite;
    SmartPtr<Sprite> _shadowNode;
    WeakPtr<FontAtlas> _fontAtlas;
    int* _horizontalKernings;

    EventListenerCustom* _purgeTextureListener;
    EventListenerCustom* _resetTextureListener;

#if CC_LABEL_DEBUG_DRAW
    DrawNode* _debugDrawNode;
#endif
    SmartPtr<SpriteFrame> _fntSpriteFrame;
    DrawNode* _underlineNode;
    //! used for optimization
    SmartPtr<Sprite> _reusedLetter;

    TTFConfig _fontConfig;
    Rect _reusedRect;
    Size _labelDimensions;

    Color4F _effectColorF;
    Color4B _textColor;
    Color4F _textColorF;
	Color4F _textGradientStartColor;
	Color4F _textGradientEndColor;

    Mat4  _shadowTransform;
    GLuint _uniformEffectColor;
    GLuint _uniformTextColor;

    Size _shadowOffset;

    Color4F _shadowColor4F;
    Color3B _shadowColor3B;
    uint8_t _shadowOpacity;

    BlendFunc _blendFunc;

    int _numberOfLines;
    int _lengthOfString;

    float _outlineSize;
    float _systemFontSize;

    //layout relevant properties.
    float _lineHeight;
    float _lineSpacing;
    float _additionalKerning;

    float _maxLineWidth;

    float _labelWidth;
    float _labelHeight;

    float _textDesiredHeight;
    float _letterOffsetY;
    float _tailoredTopY;
    float _tailoredBottomY;

    float _shadowBlurRadius;
    float _bmFontSize;
    float _bmfontScale;
    float _originalFontSize;

	float _gradientAngle;

    Flag _currLabelEffect;
    LabelType _currentLabelType;
    Overflow _overflow;
    TextHAlignment _hAlignment;
    TextVAlignment _vAlignment;

    bool _systemFontDirty;
    bool _contentDirty;
    bool _lineBreakWithoutSpaces;
    bool _strikethroughEnabled;

    bool _isOpacityModifyRGB;
    bool _enableWrap;
    bool _clipEnabled;
    bool _blendFuncDirty;
    bool _useDistanceField;
    bool _useA8Shader;
    bool _shadowDirty;

    SmartPtr<SpriteProgram> program_;

    COCOS_TYPE_OVERRIDE(Label);
private:
    CC_DISALLOW_COPY_AND_ASSIGN(Label);
};

// end group
/// @}

NS_CC_END

#endif /*__COCOS2D_CCLABEL_H */