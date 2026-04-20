package org.sawiq.dmcplus.client.ui;

record ScreenSections(int x, int y, int width, int height) {

    int right() {
        return this.x + this.width;
    }

    int bottom() {
        return this.y + this.height;
    }

    Row row(int offsetY, int height) {
        return new Row(this.x, this.y + offsetY, this.width, height);
    }

    record Row(int x, int y, int width, int height) {

        int right() {
            return this.x + this.width;
        }

        int centerY(int childHeight) {
            return this.y + (this.height - childHeight) / 2;
        }
    }
}
