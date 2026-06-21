export function Footer() {
  const year = new Date().getFullYear();
  return (
    <footer>
      <a className="brand" href="#top">
        C3<span>FLEX</span><sup>.com</sup>
      </a>
      <p>Независимая продакшн-студия / {year}</p>
      <div>
        <a href="#work">Работы</a>
        <a href="#contact">Контакт</a>
      </div>
    </footer>
  );
}
