const getFavorites = () => Application("Photos").favoritesAlbum().mediaItems();
const getAll = () => Application("Photos").mediaItems();

const getArchivePath = (date, filename) => {
  const [year, month, day] = date.toISOString().split('T')[0].split('-');

  const archiveFilename = /^\d{8}-/.test(filename) ? filename.split('-')[1] : filename;

  return `${year}/${year}-${month}/${year}-${month}-${day}/${year}${month}${day}-${archiveFilename}`
};

const getBasics = (item) => {
  const id = item.id();
  const filename = item.filename();
  const date = item.date();
  const favorite = item.favorite();

  const archivePath = getArchivePath(date, filename);

  return {
    id,
    date,
    favorite,
    'original-filename': filename,
    'archive-path': archivePath,
  };
};

const isCameraPhoto = (photo) => photo['original-filename'].match(/(DSCF\d{4}|R\d{7}).JPG/)

const report = (stuff) => console.log(JSON.stringify(stuff, null, 2));

function run(argv) {
  const photos = argv[0] === '--favorites' ? getFavorites() : getAll();

  const output = photos.map(getBasics).filter(isCameraPhoto);
  return JSON.stringify(output, null, 2);
}
