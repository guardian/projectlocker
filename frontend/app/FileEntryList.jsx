import React from 'react';
import GeneralListComponent from './GeneralListComponent.jsx';

class FileEntryList extends GeneralListComponent {
    constructor(props){
        super(props);
        this.endpoint = '/api/file';
        this.filterEndpoint = '/api/file/list';

        this.columns = [
            {
                header: "Id",
                key: "id",
                defaultSorting: "desc",
                dataProps: { className: 'align-right'},
                headerProps: { className: 'dashboardheader'}
            },
            GeneralListComponent.standardColumn("File path","filepath"),
            GeneralListComponent.standardColumn("Storage", "storage"),
            GeneralListComponent.standardColumn("Owner", "user"),
            GeneralListComponent.standardColumn("Version","version"),
            GeneralListComponent.dateTimeColumn("Create time","ctime"),
            GeneralListComponent.dateTimeColumn("Modification time","mtime"),
            GeneralListComponent.dateTimeColumn("Access time","atime")
        ];
    }
}

export default FileEntryList;