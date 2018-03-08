import React from 'react';
import GeneralListComponent from './GeneralListComponent.jsx';

class PostrunList extends GeneralListComponent {
    constructor(props){
        super(props);
        this.endpoint = '/api/postrun';

        this.columns = [
            {
                header: "Id",
                key: "id",
                defaultSorting: "desc",
                dataProps: { className: 'align-right'},
                headerProps: { className: 'dashboardheader'}
            },
            GeneralListComponent.standardColumn("Title","title"),
            GeneralListComponent.standardColumn("Description","description"),
            GeneralListComponent.standardColumn("Script name","runnable"),
            GeneralListComponent.standardColumn("Version","version"),
            GeneralListComponent.dateTimeColumn("Created", "ctime"),
            GeneralListComponent.standardColumn("Owner","owner"),
            this.actionIcons()
        ];
    }

}

export default PostrunList;