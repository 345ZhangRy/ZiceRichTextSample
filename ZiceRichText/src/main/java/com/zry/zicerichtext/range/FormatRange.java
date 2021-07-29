package com.zry.zicerichtext.range;

public class FormatRange extends Range {

    private FormatData convert;
    private CharSequence rangeCharSequence;

    public FormatRange(int from, int to) {
        super(from, to);
    }

    public FormatData getConvert() {
        return convert;
    }

    public void setConvert(FormatData convert) {
        this.convert = convert;
    }

    public CharSequence getRangeCharSequence() {
        return rangeCharSequence;
    }

    public void setRangeCharSequence(CharSequence rangeCharSequence) {
        this.rangeCharSequence = rangeCharSequence;
    }

    public interface FormatData {

        CharSequence formatCharSequence();

        CharSequence formatParam();
    }
}
