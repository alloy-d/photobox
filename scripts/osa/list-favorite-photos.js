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

  const archivePath = getArchivePath(date, filename);

  return {
    'apple-photos-id': id,
    filename,
    date,
    'archive-path': archivePath,
  };
};

const isCameraPhoto = ({filename}) => filename.match(/(DSCF\d{4}|R\d{7}).JPG/)

const report = (stuff) => console.log(JSON.stringify(stuff, null, 2));

function run(argv) {
  const photos = argv[0] === '--all' ? getAll() : getFavorites();

  const output = photos.map(getBasics).filter(isCameraPhoto);
  return JSON.stringify(output, null, 2);
}
