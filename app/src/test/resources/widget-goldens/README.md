# Widget migration references

These 20 mdpi, English reference renders and text bounds were captured from the
RemoteViews widgets at commit `f623549`, before their Glance migration. They cover
all three widgets, light/dark themes, compact/expanded sizes and stacked splits.

The capture used Robolectric API 34 with native graphics and the purple palette.
Because software `View.draw()` does not apply hardware outline clipping, the
original child outline radii were applied to the reference images explicitly.

`GlanceWidgetRenderingTest` compares both text geometry and raster output. A
two-channel-value tolerance and at most four differing edge pixels accommodate
software compositing at rounded corners. Update references only after reviewing
an intentional visual change; do not regenerate them from a failing renderer.
