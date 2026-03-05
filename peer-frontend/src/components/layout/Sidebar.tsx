"use client";

import Image from "next/image";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/contexts/AuthContext";

const navItems = [
  { href: "/scheduler", label: "Calendar", icon: "📅" },
  { href: "/todos", label: "Todos", icon: "✅" },
  { href: "/algobank", label: "AlgoBank", icon: "💻" },
  { href: "/community", label: "Community", icon: "💬" },
  { href: "/notifications", label: "Notifications", icon: "🔔" },
  { href: "/profile", label: "Profile", icon: "👤" },
];

const adminNavItem = { href: "/admin", label: "Admin", icon: "🛡️" };

export default function Sidebar() {
  const pathname = usePathname();
  const { user, logout } = useAuth();

  return (
    <aside className="w-64 bg-gray-900 text-white min-h-screen flex flex-col">
      <div className="p-6 border-b border-gray-700">
        <Link href="/scheduler" className="flex items-center gap-3">
          <Image src="/logo.png" alt="PEER" width={48} height={28} className="rounded" />
          <div>
            <span className="text-2xl font-bold tracking-wide">PEER</span>
            <p className="text-xs text-gray-400">Work with Peer in Life</p>
          </div>
        </Link>
      </div>

      <nav className="flex-1 p-4 space-y-1">
        {navItems.map((item) => {
          const isActive = pathname.startsWith(item.href);
          return (
            <Link
              key={item.href}
              href={item.href}
              className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${
                isActive
                  ? "bg-blue-600 text-white"
                  : "text-gray-300 hover:bg-gray-800 hover:text-white"
              }`}
            >
              <span>{item.icon}</span>
              <span>{item.label}</span>
            </Link>
          );
        })}
        {user?.role === "ADMIN" && (
          <Link
            href={adminNavItem.href}
            className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${
              pathname.startsWith("/admin")
                ? "bg-blue-600 text-white"
                : "text-gray-300 hover:bg-gray-800 hover:text-white"
            }`}
          >
            <span>{adminNavItem.icon}</span>
            <span>{adminNavItem.label}</span>
          </Link>
        )}
      </nav>

      {user && (
        <div className="p-4 border-t border-gray-700">
          <div className="flex items-center gap-3 mb-3">
            {user.profileImageUrl ? (
              <img
                src={user.profileImageUrl}
                alt={user.name}
                className="w-8 h-8 rounded-full"
              />
            ) : (
              <div className="w-8 h-8 rounded-full bg-blue-600 flex items-center justify-center text-sm font-bold">
                {user.name[0]}
              </div>
            )}
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium truncate">{user.name}</p>
              <p className="text-xs text-gray-400">
                Lv.{user.level} · {user.totalXp} XP
              </p>
            </div>
          </div>
          <button
            onClick={logout}
            className="w-full text-sm text-gray-400 hover:text-white transition-colors text-left"
          >
            Logout
          </button>
        </div>
      )}
    </aside>
  );
}
