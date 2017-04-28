import React from 'react';
import GeneralListComponent from './GeneralListComponent.jsx';

class FileEntryList extends GeneralListComponent {
    constructor(props){
        super(props);
        this.endpoint = '/file';
        this.columns = [
            {
                header: "Id",
                key: "id",
                defaultSorting: "desc",
                dataProps: { className: 'align-right'},
                headerProps: { className: 'dashboardheader'}
            },
            GeneralListComponent.standardColumn("File path","filepath¡"),
            GeneralListComponent.standardColumn("Storage", "storage"),
            GeneralListComponent.standardColumn("Owner", "user"),
            GeneralListComponent.standardColumn("Version","version"),
            GeneralListComponent.standardColumn("Create time","ctime"),
            GeneralListComponent.standardColumn("Modification time","mtime"),
            GeneralListComponent.standardColumn("Access time","atime")
        ];
    }
}

export default FileEntryList;