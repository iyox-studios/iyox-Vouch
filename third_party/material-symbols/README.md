# Material Symbols

The `ic_*.xml` Material Symbols assets in `app/src/main/res/drawable` are from
Google's [material-design-icons](https://github.com/google/material-design-icons)
repository, revision `10ecc14790612ca8fd164e3f4ffe9c16fc50d5b7`, under Apache-2.0 (see LICENSE).

Style: Material Symbols Outlined, weight 400, optical size 24dp. Navigation
selected states use FILL=1; other icons use FILL=0. Each XML records its source.
Theme tint was removed so Compose controls tint. Previously mirrored icons retain
`android:autoMirrored="true"`.

To add an icon, download Android XML from https://fonts.google.com/icons, put it
in `app/src/main/res/drawable/ic_name.xml`, and use
`Icon(painter = painterResource(R.drawable.ic_name), contentDescription = ...)`.
Use the same style, weight, and size; remove theme tint and enable autoMirrored
where appropriate for directional navigation. SVG downloads must first be imported
as a vector drawable using Android Studio's New > Vector Asset > Local file.
