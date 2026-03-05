"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { useAuth } from "@/contexts/AuthContext";
import { User, Post, Page } from "@/types";

type Tab = "users" | "reported" | "blinded";

export default function AdminPage() {
  const { user, loading } = useAuth();
  const router = useRouter();
  const [tab, setTab] = useState<Tab>("users");
  const [users, setUsers] = useState<User[]>([]);
  const [reportedPosts, setReportedPosts] = useState<Post[]>([]);
  const [blindedPosts, setBlindedPosts] = useState<Post[]>([]);
  const [userPage, setUserPage] = useState(0);
  const [userTotalPages, setUserTotalPages] = useState(0);
  const [reportedPage, setReportedPage] = useState(0);
  const [reportedTotalPages, setReportedTotalPages] = useState(0);
  const [blindedPage, setBlindedPage] = useState(0);
  const [blindedTotalPages, setBlindedTotalPages] = useState(0);

  useEffect(() => {
    if (!loading && (!user || user.role !== "ADMIN")) {
      router.replace("/scheduler");
    }
  }, [user, loading, router]);

  const fetchUsers = (p: number) => {
    api.get<Page<User>>(`/api/admin/users?page=${p}&size=20`).then((data) => {
      setUsers(data.content);
      setUserTotalPages(data.totalPages);
    });
  };

  const fetchReported = (p: number) => {
    api.get<Page<Post>>(`/api/admin/posts/reported?page=${p}&size=20`).then((data) => {
      setReportedPosts(data.content);
      setReportedTotalPages(data.totalPages);
    });
  };

  const fetchBlinded = (p: number) => {
    api.get<Page<Post>>(`/api/admin/posts/blinded?page=${p}&size=20`).then((data) => {
      setBlindedPosts(data.content);
      setBlindedTotalPages(data.totalPages);
    });
  };

  useEffect(() => {
    if (user?.role === "ADMIN") {
      fetchUsers(userPage);
    }
  }, [userPage, user]);

  useEffect(() => {
    if (user?.role === "ADMIN") {
      fetchReported(reportedPage);
    }
  }, [reportedPage, user]);

  useEffect(() => {
    if (user?.role === "ADMIN") {
      fetchBlinded(blindedPage);
    }
  }, [blindedPage, user]);

  const handlePromote = async (userId: number) => {
    await api.patch(`/api/admin/users/${userId}/promote`);
    fetchUsers(userPage);
  };

  const handleDemote = async (userId: number) => {
    await api.patch(`/api/admin/users/${userId}/demote`);
    fetchUsers(userPage);
  };

  const handleUnblind = async (postId: number) => {
    await api.patch(`/api/admin/posts/${postId}/unblind`);
    fetchReported(reportedPage);
    fetchBlinded(blindedPage);
  };

  const handleDelete = async (postId: number) => {
    if (!confirm("Are you sure you want to delete this post?")) return;
    await api.delete(`/api/admin/posts/${postId}`);
    fetchReported(reportedPage);
    fetchBlinded(blindedPage);
  };

  if (loading || !user || user.role !== "ADMIN") {
    return null;
  }

  const tabs: { key: Tab; label: string }[] = [
    { key: "users", label: "Users" },
    { key: "reported", label: "Reported Posts" },
    { key: "blinded", label: "Blinded Posts" },
  ];

  return (
    <div>
      <h1 className="text-2xl font-bold text-white mb-6">Admin Dashboard</h1>

      <div className="flex gap-2 mb-6">
        {tabs.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              tab === t.key
                ? "bg-blue-600 text-white"
                : "bg-gray-800 text-gray-400 hover:bg-gray-700"
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === "users" && (
        <div>
          <div className="bg-gray-900 rounded-xl overflow-hidden">
            <table className="w-full text-left">
              <thead className="bg-gray-800">
                <tr>
                  <th className="px-4 py-3 text-xs text-gray-400 font-medium">ID</th>
                  <th className="px-4 py-3 text-xs text-gray-400 font-medium">Name</th>
                  <th className="px-4 py-3 text-xs text-gray-400 font-medium">Email</th>
                  <th className="px-4 py-3 text-xs text-gray-400 font-medium">Role</th>
                  <th className="px-4 py-3 text-xs text-gray-400 font-medium">Level</th>
                  <th className="px-4 py-3 text-xs text-gray-400 font-medium">Joined</th>
                  <th className="px-4 py-3 text-xs text-gray-400 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-800">
                {users.map((u) => (
                  <tr key={u.id} className="hover:bg-gray-800/50">
                    <td className="px-4 py-3 text-sm text-gray-300">{u.id}</td>
                    <td className="px-4 py-3 text-sm text-white">{u.name}</td>
                    <td className="px-4 py-3 text-sm text-gray-400">{u.email}</td>
                    <td className="px-4 py-3">
                      <span
                        className={`text-xs px-2 py-0.5 rounded ${
                          u.role === "ADMIN"
                            ? "bg-red-600/20 text-red-400"
                            : "bg-blue-600/20 text-blue-400"
                        }`}
                      >
                        {u.role}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-300">Lv.{u.level}</td>
                    <td className="px-4 py-3 text-sm text-gray-500">
                      {new Date(u.createdAt).toLocaleDateString()}
                    </td>
                    <td className="px-4 py-3">
                      {u.id !== user.id && (
                        u.role === "USER" ? (
                          <button
                            onClick={() => handlePromote(u.id)}
                            className="text-xs bg-red-600/20 text-red-400 px-2 py-1 rounded hover:bg-red-600/30"
                          >
                            Promote
                          </button>
                        ) : (
                          <button
                            onClick={() => handleDemote(u.id)}
                            className="text-xs bg-gray-600/20 text-gray-400 px-2 py-1 rounded hover:bg-gray-600/30"
                          >
                            Demote
                          </button>
                        )
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={userPage} totalPages={userTotalPages} onPageChange={setUserPage} />
        </div>
      )}

      {tab === "reported" && (
        <div>
          <div className="space-y-3">
            {reportedPosts.map((post) => (
              <PostCard
                key={post.id}
                post={post}
                onUnblind={handleUnblind}
                onDelete={handleDelete}
                showReportCount
              />
            ))}
            {reportedPosts.length === 0 && (
              <div className="text-center text-gray-500 py-12">No reported posts.</div>
            )}
          </div>
          <Pagination page={reportedPage} totalPages={reportedTotalPages} onPageChange={setReportedPage} />
        </div>
      )}

      {tab === "blinded" && (
        <div>
          <div className="space-y-3">
            {blindedPosts.map((post) => (
              <PostCard
                key={post.id}
                post={post}
                onUnblind={handleUnblind}
                onDelete={handleDelete}
              />
            ))}
            {blindedPosts.length === 0 && (
              <div className="text-center text-gray-500 py-12">No blinded posts.</div>
            )}
          </div>
          <Pagination page={blindedPage} totalPages={blindedTotalPages} onPageChange={setBlindedPage} />
        </div>
      )}
    </div>
  );
}

function PostCard({
  post,
  onUnblind,
  onDelete,
  showReportCount,
}: {
  post: Post;
  onUnblind: (id: number) => void;
  onDelete: (id: number) => void;
  showReportCount?: boolean;
}) {
  return (
    <div className="bg-gray-900 rounded-xl p-5">
      <div className="flex items-start justify-between">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-3 mb-1">
            <span className="text-xs px-2 py-0.5 rounded bg-gray-700 text-gray-300">
              {post.tag}
            </span>
            <h3 className="text-lg font-semibold text-white truncate">{post.title}</h3>
          </div>
          <p className="text-gray-400 text-sm line-clamp-2 mb-2">{post.content}</p>
          <div className="flex items-center gap-4 text-xs text-gray-500">
            <span>{post.authorName}</span>
            <span>{new Date(post.createdAt).toLocaleDateString()}</span>
            {showReportCount && (
              <span className="text-red-400">Reports: {post.reportCount}</span>
            )}
            {post.blinded && (
              <span className="text-yellow-400">Blinded</span>
            )}
          </div>
        </div>
        <div className="flex gap-2 ml-4 shrink-0">
          {post.blinded && (
            <button
              onClick={() => onUnblind(post.id)}
              className="text-xs bg-green-600/20 text-green-400 px-3 py-1.5 rounded hover:bg-green-600/30"
            >
              Unblind
            </button>
          )}
          <button
            onClick={() => onDelete(post.id)}
            className="text-xs bg-red-600/20 text-red-400 px-3 py-1.5 rounded hover:bg-red-600/30"
          >
            Delete
          </button>
        </div>
      </div>
    </div>
  );
}

function Pagination({
  page,
  totalPages,
  onPageChange,
}: {
  page: number;
  totalPages: number;
  onPageChange: (p: number) => void;
}) {
  if (totalPages <= 1) return null;
  return (
    <div className="flex justify-center gap-2 mt-6">
      {Array.from({ length: totalPages }, (_, i) => (
        <button
          key={i}
          onClick={() => onPageChange(i)}
          className={`px-3 py-1 rounded ${
            i === page ? "bg-blue-600 text-white" : "bg-gray-800 text-gray-400 hover:bg-gray-700"
          }`}
        >
          {i + 1}
        </button>
      ))}
    </div>
  );
}
