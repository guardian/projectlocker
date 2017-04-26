import React from 'react';
import SortableTable from 'react-sortable-table';
import GeneralListComponent from './GeneralListComponent.jsx';

class StorageListComponent extends GeneralListComponent {
    constructor(props){
        super(props);
        this.endpoint = '/storage';
    }

    render() {
        const columns = [
            {
                header: "Id",
                key: "id",
                defaultSorting: "desc",
                dataProps: { className: 'align-right'}
            },
            {
                header: "Type",
                key: "type"
            },
            {
                header: "Root path",
                key: "rootpath"
            },
            {
                header: "Storage type",
                key: "storageType"
            },
            {
                header: "User",
                key: "user"
            },
            {
                header: "Password",
                key: "password"
            },
            {
                header: "Host",
                key: "host"
            },
            {
                header: "Port",
                key: "port"
            }
        ];

        const style = {
            backgroundColor: '#eee'
        };

        const iconStyle = {
            color: '#aaa',
            paddingLeft: '5px',
            paddingRight: '5px'
        };

        return (
            <SortableTable
                data={ this.state.data}
                columns={columns}
                style={style}
                iconStyle={iconStyle}
                />
        );

    }
}

export default StorageListComponent;