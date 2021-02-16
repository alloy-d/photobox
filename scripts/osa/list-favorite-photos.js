const getFavorites = () => Application("Photos").favoritesAlbum().mediaItems();

const getArchivePath = (date, filename) => {
  const [year, month, day] = date.toISOString().split('T')[0].split('-');

  return `${year}/${year}-${month}/${year}-${month}-${day}/${year}${month}${day}-${filename}`
};

const getBasics = (item) => {
  const id = item.id();
  const filename = item.filename();
  const date = item.date();

  const archivePath = getArchivePath(date, filename);

  return {
    id,
    filename,
    date,
    archivePath,
  };
};

const isCameraPhoto = ({filename}) => filename.match(/(DSCF\d{4}|R\d{7}).JPG/)

const report = (stuff) => console.log(JSON.stringify(stuff, null, 2));
report(getFavorites().map(getBasics).filter(isCameraPhoto));
