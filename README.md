# photobox

Hi! This is a repository for my own personal workflow for importing
photos.  It's cool, and it might be useful to you with a little
tweaking, but I make no promises.

## My current workflow

1. Take photos.
2. At opportune moments (often while riding the subway), use my camera's
   UI to give a rating of 4 or 5 stars to any photo I like.
3. Import photos using this tool, which archives them all to a NAS
   device and collects the 4–5-star ones in a folder on my desktop.
4. Import 4–5 star photos from my desktop to a "good photos" album in
   macOS's Photos app, which syncs them to my other devices.
5. Periodically format my SD cards through my camera's UI.

## History

I used to use Lightroom, but over time I realized that I didn't enjoy
developing photos in Lightroom.  And so for a while, I wound up just
using Lightroom to import photos.

As a glorified file copying tool, Lightroom is slow, heavy, and
expensive.  Also, the process it led to was something like this:

1. Take photos.
2. Import photos using Lightroom.
3. Look through the JPEG versions of _all the photos_ that were just
   imported, figure out which ones I like, and give them ratings.
4. Select the photos I had rated highly, and export them to a folder
   somewhere.
5. Import those exported photos to macOS's Photos app.

This process had a number of things I didn't like.  Mainly:

- It required me to synchronously look through _all of the imported_
  photos to decide which ones I liked.  Often, I didn't feel like doing
  that, and I half-assed it.
- My most common use case was getting syncing a particular photo or
  group of photos to my phone, but this workflow required that to be the
  absolute last step.
- This required my NAS device (where my photos are stored) being
  available and mounted to my machine, which meant that I couldn't even
  _think_ about importing a photo without being at home.

but also:

- Lightroom typically treats JPEGs and raw files as a single photo.
  This wasn't what I wanted, so I had to configure Lightroom to treat
  them as _completely separate_ photos. This made the review process
  clunky.
- Ratings were only applied (as far as I understand) within Lightroom's
  database, not to the files themselves.  Also, giving a rating to
  a JPEG file didn't apply that rating to the corresponding raw file.
- It required an "export" step to turn JPEG files in Lightroom's catalog into
  JPEG files _somewhere else_, and that felt really silly.

I realized that I could make my own tool that was faster and lighter and
fit my workflow better.

## How I use this tool

It uses [boot][boot].  My entry point here is

```sh
$ boot process-photos
```

which does the following (in this order):

- copies any JPEG files I've rated 4 stars or higher (in my camera's UI)
  to a folder on my desktop named `good photos`.
- copies any JPEG files I've rated 5 stars to a folder named `great
  photos`.
- archives all JPEG and RAF files to my NAS device, if it's mounted, in
  the same date-based hierarchy that I had configured Lightroom to use.

## How it works

Basically, it's a bunch of transducing that turns a directory of photo
files into a list of actions, then does them.

My pipeline is [hard-coded in
`photobox.core`](./src/photobox/core.clj#L34-L40).  Each of the
`transductions` functions takes the full list of photos and produces
a list of plans.

A "plan" represents intent to take an action—something like "I want to
copy `A.jpeg` to `good-photos/A.jpeg`".  A plan is "assessed" ("are the
conditions such that I _can_ I copy `A.jpeg` to `good-photos/A.jpeg`?")
and then, if it's "doable" (e.g., not impossible or a no-op), "executed"
("now I will copy `A.jpeg` to `good-photos/A.jpeg`!").

So: this makes a bunch of plans, then assesses and executes them in
bulk.  That's it.

If you're interested in more detail, I'd recommend cloning this and
running `boot docs` to generate some nice [Marginalia][marginalia]
output in `target/docs/uberdoc.html`.

[boot]: http://boot-clj.com/
[marginalia]: https://github.com/gdeer81/marginalia
