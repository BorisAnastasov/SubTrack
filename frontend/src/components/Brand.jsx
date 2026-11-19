import { Link } from "react-router-dom";
import logo from "../../assets/logo.png";

export function Brand({ compact = false }) {
  return (
    <Link className={`brand${compact ? " brand--compact" : ""}`} to="/">
      <img className="brand-logo" src={logo} alt="" />
      <span>subtrack</span>
    </Link>
  );
}
